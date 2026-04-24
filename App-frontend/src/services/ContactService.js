import * as Contacts from 'expo-contacts';

/**
 * Normalizes phone numbers to a 10-digit format.
 * Strips non-numeric characters and removes the '91' prefix if present.
 */
export const normalizePhoneNumber = (phone) => {
  if (!phone) return null;
  
  // Remove all non-digit characters
  let cleaned = phone.replace(/\D/g, '');
  
  // If it starts with 91 and is longer than 10 digits, remove the 91 prefix
  if (cleaned.length > 10 && cleaned.startsWith('91')) {
    cleaned = cleaned.substring(cleaned.length - 10);
  }
  
  // Return the last 10 digits if it's a valid length, otherwise return null or original
  if (cleaned.length === 10) {
    return cleaned;
  }
  
  return null; // Ignore invalid formats for sync
};

/**
 * Requests contact permissions from the user.
 */
export const requestContactPermission = async () => {
  const { status } = await Contacts.requestPermissionsAsync();
  return status === 'granted';
};

/**
 * Fetches and normalizes all contacts from the device.
 */
export const getDeviceContacts = async () => {
  const hasPermission = await requestContactPermission();
  if (hasPermission) {
    const { data } = await Contacts.getContactsAsync({
      fields: [Contacts.Fields.Name, Contacts.Fields.PhoneNumbers],
    });

    if (data.length > 0) {
      const contactMap = new Map();
      
      data.forEach(contact => {
        if (contact.phoneNumbers) {
          contact.phoneNumbers.forEach(p => {
            const normalized = normalizePhoneNumber(p.number);
            if (normalized) {
              // Store unique normalized numbers
              contactMap.set(normalized, {
                id: contact.id,
                name: contact.name,
                phoneNumber: normalized,
                originalNumber: p.number
              });
            }
          });
        }
      });
      
      return Array.from(contactMap.values());
    }
  }
  return [];
};
