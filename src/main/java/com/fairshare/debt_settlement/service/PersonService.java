package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.CreatePersonRequest;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PersonService {

    public final PersonRepository personRepository;
    public final DebtRepository debtRepository;

    private Person getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User profile not found in database"));
    }

    @org.springframework.transaction.annotation.Transactional
    public Person addPerson(CreatePersonRequest request) {
        Person currentUser = getCurrentUser();
        Person friendToAdd;

        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
            Optional<Person> existingPerson = personRepository.findByPhoneNumber(normalizedPhone);
            if (existingPerson.isPresent()) {
                friendToAdd = existingPerson.get();
            } else {
                friendToAdd = new Person();
                friendToAdd.setName(request.getName().trim());
                friendToAdd.setPhoneNumber(normalizedPhone);
                // Assign a temporary email since it is required and unique
                friendToAdd.setEmail("phone_" + normalizedPhone + "@cleardues.local");
                friendToAdd = personRepository.save(friendToAdd);
                personRepository.flush(); // Ensure ID is generated for friendship link
            }
        } else if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<Person> existingPerson = personRepository.findByEmail(request.getEmail().trim());
            if (existingPerson.isPresent()) {
                friendToAdd = existingPerson.get();
            } else {
                // Create placeholder person with real email
                friendToAdd = new Person();
                friendToAdd.setName(request.getName().trim());
                friendToAdd.setEmail(request.getEmail().trim());
                friendToAdd = personRepository.save(friendToAdd);
                personRepository.flush();
            }
        } else {
            // Unregistered proxy person
            friendToAdd = new Person();
            friendToAdd.setName(request.getName().trim());
            friendToAdd.setEmail("offline_" + java.util.UUID.randomUUID().toString() + "@cleardues.local");
            friendToAdd = personRepository.save(friendToAdd);
            personRepository.flush();
        }

        establishMutualFriendship(currentUser, friendToAdd);
        return friendToAdd;
    }

    @org.springframework.transaction.annotation.Transactional
    public List<Person> syncContactsBatch(List<CreatePersonRequest> contacts) {
        Person currentUser = getCurrentUser();
        java.util.Set<String> processedNumbers = new java.util.HashSet<>();

        for (CreatePersonRequest contact : contacts) {
            String normalizedPhone = normalizePhoneNumber(contact.getPhoneNumber());
            if (normalizedPhone == null || normalizedPhone.isEmpty()) continue;
            
            // Avoid processing the same number twice in one batch to prevent unique constraint errors
            if (processedNumbers.contains(normalizedPhone)) continue; 
            processedNumbers.add(normalizedPhone);

            Person friendToAdd;
            Optional<Person> existingPerson = personRepository.findByPhoneNumber(normalizedPhone);
            
            if (existingPerson.isPresent()) {
                friendToAdd = existingPerson.get();
                establishMutualFriendship(currentUser, friendToAdd);
            }
            // Skip unregistered contacts - we don't auto-add them as friends anymore
        }
        
        if (currentUser != null) {
            personRepository.save(currentUser);
        }
        return getAllPersons();
    }

    private void establishMutualFriendship(Person a, Person b) {
        if (a.getId().equals(b.getId())) return;
        
        boolean alreadyFriends = a.getFriends().stream()
                .anyMatch(f -> f.getId().equals(b.getId()));
        
        if (!alreadyFriends) {
            a.getFriends().add(b);
            b.getFriends().add(a);
            // Relationship is @ManyToMany, saving either one updates the join table
        }
    }

    public List<java.util.Map<String, Object>> checkContacts(List<String> phoneNumbers) {
        List<String> normalizedNumbers = phoneNumbers.stream()
                .map(this::normalizePhoneNumber)
                .filter(n -> n != null && !n.isEmpty())
                .collect(java.util.stream.Collectors.toList());

        List<Person> foundPersons = personRepository.findAllByPhoneNumberIn(normalizedNumbers);
        java.util.Map<String, Person> phoneToPerson = foundPersons.stream()
                .collect(java.util.stream.Collectors.toMap(Person::getPhoneNumber, p -> p));

        List<java.util.Map<String, Object>> results = new ArrayList<>();
        for (String original : phoneNumbers) {
            String normalized = normalizePhoneNumber(original);
            Person p = phoneToPerson.get(normalized);
            
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("original", original);
            map.put("normalized", normalized);
            map.put("registered", p != null);
            if (p != null) {
                map.put("name", p.getName());
            }
            results.add(map);
        }
        return results;
    }

    public String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        String cleaned = phone.replaceAll("\\D", ""); // Remove non-digits
        if (cleaned.length() > 10 && (cleaned.startsWith("91"))) {
            return cleaned.substring(cleaned.length() - 10);
        }
        return cleaned;
    }

    @org.springframework.transaction.annotation.Transactional
    public Person updateMyPhone(String phone) {
        Person currentUser = getCurrentUser();
        String normalized = normalizePhoneNumber(phone);
        if (normalized == null || normalized.length() != 10) {
            throw new RuntimeException("Invalid phone number format. Please enter 10 digits.");
        }

        // Check if there is another person with this phone number
        Optional<Person> existingPersonOpt = personRepository.findByPhoneNumber(normalized);
        if (existingPersonOpt.isPresent()) {
            Person existingPerson = existingPersonOpt.get();
            if (!existingPerson.getId().equals(currentUser.getId())) {
                // If the existing user is a placeholder/proxy, we merge them
                if (existingPerson.getEmail().endsWith("@cleardues.local")) {
                    
                    // 1. Migrate friendships
                    for (Person friend : new ArrayList<>(existingPerson.getFriends())) {
                        friend.getFriends().remove(existingPerson);
                        existingPerson.getFriends().remove(friend);
                        personRepository.save(friend);
                        
                        establishMutualFriendship(currentUser, friend);
                    }
                    
                    // 2. Migrate debts
                    List<Debt> debtorDebts = debtRepository.findByDebtorId(existingPerson.getId());
                    for (Debt debt : debtorDebts) {
                        debt.setDebtor(currentUser);
                    }
                    debtRepository.saveAll(debtorDebts);

                    List<Debt> creditorDebts = debtRepository.findByCreditorId(existingPerson.getId());
                    for (Debt debt : creditorDebts) {
                        debt.setCreditor(currentUser);
                    }
                    debtRepository.saveAll(creditorDebts);
                    
                    debtRepository.flush();

                    // 3. Delete the proxy person
                    personRepository.delete(existingPerson);
                    personRepository.flush();
                } else {
                    throw new RuntimeException("This phone number is already registered by another user.");
                }
            }
        }

        currentUser.setPhoneNumber(normalized);
        return personRepository.save(currentUser);
    }

    public List<Person> getAllPersons() {
        Person currentUser = getCurrentUser();
        // Exclude the current user themselves if they happen to be in the set
        List<Person> friends = new ArrayList<>(currentUser.getFriends());
        friends.removeIf(p -> p.getId().equals(currentUser.getId()));
        return friends;
    }

    public void deletePerson(Long friendId) {
        if (friendId == null) throw new IllegalArgumentException("Friend ID must not be null");
        Person currentUser = getCurrentUser();
        Person friendToDelete = personRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        // 1. Remove mutual friendship
        currentUser.getFriends().remove(friendToDelete);
        friendToDelete.getFriends().remove(currentUser);
        personRepository.save(currentUser);
        
        // 2. Delete mutual debts
        debtRepository.deleteMutualDebts(currentUser.getId(), friendToDelete.getId());

        // 3. If the user is an offline proxy user, we delete them entirely
        if (friendToDelete.getEmail().endsWith("@cleardues.local")) {
            personRepository.delete(friendToDelete);
        } else {
            personRepository.save(friendToDelete);
        }
    }
}
