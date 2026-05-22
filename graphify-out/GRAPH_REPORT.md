# Graph Report - .  (2026-05-22)

## Corpus Check
- 53 files · ~92,196 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 169 nodes · 175 edges · 22 communities detected
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 27 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_User Profile Management|User Profile Management]]
- [[_COMMUNITY_JWT & Security Filter|JWT & Security Filter]]
- [[_COMMUNITY_Person & Debt Persistence|Person & Debt Persistence]]
- [[_COMMUNITY_Debt Settlement Logic|Debt Settlement Logic]]
- [[_COMMUNITY_Transaction Queries|Transaction Queries]]
- [[_COMMUNITY_Friendship & Person Service|Friendship & Person Service]]
- [[_COMMUNITY_Person API Controller|Person API Controller]]
- [[_COMMUNITY_Exception Handling|Exception Handling]]
- [[_COMMUNITY_Debt API Controller|Debt API Controller]]
- [[_COMMUNITY_Global Error Responses|Global Error Responses]]
- [[_COMMUNITY_SMS Notification Service|SMS Notification Service]]
- [[_COMMUNITY_Branding & Project Overview|Branding & Project Overview]]
- [[_COMMUNITY_Device Contact Integration|Device Contact Integration]]
- [[_COMMUNITY_Security Configuration|Security Configuration]]
- [[_COMMUNITY_Spring Boot Entry Point|Spring Boot Entry Point]]
- [[_COMMUNITY_Backend Unit Testing|Backend Unit Testing]]
- [[_COMMUNITY_Debt Creation DTO|Debt Creation DTO]]
- [[_COMMUNITY_Person Creation DTO|Person Creation DTO]]
- [[_COMMUNITY_Settlement Response DTO|Settlement Response DTO]]
- [[_COMMUNITY_Debt Model Entity|Debt Model Entity]]
- [[_COMMUNITY_Person Model Entity|Person Model Entity]]
- [[_COMMUNITY_Package Name Fix|Package Name Fix]]

## God Nodes (most connected - your core abstractions)
1. `PersonController` - 10 edges
2. `PersonService` - 10 edges
3. `JwtService` - 9 edges
4. `DebtRepository` - 8 edges
5. `DebtService` - 8 edges
6. `DebtController` - 5 edges
7. `SettlementController` - 4 edges
8. `GlobalExceptionHandler` - 4 edges
9. `PersonRepository` - 4 edges
10. `OAuth2LoginSuccessHandler` - 4 edges

## Surprising Connections (you probably didn't know these)
- `ClearDues Logo Branding` --represents--> `Clear Dues Project`  [INFERRED]
  App-frontend/assets/logo.png → README.md

## Communities

### Community 0 - "User Profile Management"
Cohesion: 0.15
Nodes (4): ProfileController, ApplicationConfig, OAuth2LoginSuccessHandler, SimpleUrlAuthenticationSuccessHandler

### Community 1 - "JWT & Security Filter"
Cohesion: 0.25
Nodes (3): OncePerRequestFilter, JwtAuthenticationFilter, JwtService

### Community 2 - "Person & Debt Persistence"
Cohesion: 0.21
Nodes (2): PersonRepository, DebtService

### Community 3 - "Debt Settlement Logic"
Cohesion: 0.18
Nodes (3): SettlementController, PersonBalance, SettlementService

### Community 4 - "Transaction Queries"
Cohesion: 0.18
Nodes (1): DebtRepository

### Community 5 - "Friendship & Person Service"
Cohesion: 0.4
Nodes (1): PersonService

### Community 6 - "Person API Controller"
Cohesion: 0.22
Nodes (1): PersonController

### Community 7 - "Exception Handling"
Cohesion: 0.29
Nodes (3): BadRequestException, ResourceNotFoundException, RuntimeException

### Community 8 - "Debt API Controller"
Cohesion: 0.33
Nodes (1): DebtController

### Community 9 - "Global Error Responses"
Cohesion: 0.4
Nodes (1): GlobalExceptionHandler

### Community 10 - "SMS Notification Service"
Cohesion: 0.5
Nodes (1): SmsService

### Community 11 - "Branding & Project Overview"
Cohesion: 0.4
Nodes (5): ClearDues Logo Branding, Smart Debt Settlement Algorithm, Clear Dues Project, Glassmorphism Design System, OAuth2 + JWT Bridge

### Community 13 - "Device Contact Integration"
Cohesion: 0.67
Nodes (2): getDeviceContacts(), requestContactPermission()

### Community 14 - "Security Configuration"
Cohesion: 0.5
Nodes (1): SecurityConfig

### Community 15 - "Spring Boot Entry Point"
Cohesion: 0.67
Nodes (1): DebtSettlementApplication

### Community 16 - "Backend Unit Testing"
Cohesion: 0.67
Nodes (1): DebtSettlementApplicationTests

### Community 27 - "Debt Creation DTO"
Cohesion: 1.0
Nodes (1): CreateDebtRequest

### Community 28 - "Person Creation DTO"
Cohesion: 1.0
Nodes (1): CreatePersonRequest

### Community 29 - "Settlement Response DTO"
Cohesion: 1.0
Nodes (1): SettlementResponse

### Community 30 - "Debt Model Entity"
Cohesion: 1.0
Nodes (1): Debt

### Community 31 - "Person Model Entity"
Cohesion: 1.0
Nodes (1): Person

### Community 41 - "Package Name Fix"
Cohesion: 1.0
Nodes (1): Package Name Correction

## Knowledge Gaps
- **10 isolated node(s):** `CreateDebtRequest`, `CreatePersonRequest`, `SettlementResponse`, `Debt`, `Person` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Person & Debt Persistence`** (12 nodes): `.checkPersonExists()`, `PersonRepository`, `.findAllByPhoneNumberIn()`, `.findByPhoneNumber()`, `DebtService`, `.createDebt()`, `.deleteDebt()`, `.getAllDebts()`, `.normalizePhone()`, `.updateDebt()`, `PersonRepository.java`, `DebtService.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Transaction Queries`** (11 nodes): `DebtRepository`, `.deleteMutualDebts()`, `.findAllPendingTransactionsForUser()`, `.findAllTransactionsForUser()`, `.findByCreditorId()`, `.findByDebtorId()`, `.getTotalOwedByUser()`, `.getTotalOwedToUser()`, `.getTotalOwed()`, `.getTotalReceivable()`, `DebtRepository.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Friendship & Person Service`** (11 nodes): `PersonService`, `.addPerson()`, `.checkContacts()`, `.deletePerson()`, `.establishMutualFriendship()`, `.getAllPersons()`, `.getCurrentUser()`, `.normalizePhoneNumber()`, `.syncContactsBatch()`, `.updateMyPhone()`, `PersonService.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Person API Controller`** (9 nodes): `PersonController`, `.addPerson()`, `.checkBatchContacts()`, `.deletePerson()`, `.getAllPersons()`, `.PersonController()`, `.syncBatchContacts()`, `.updateMyPhone()`, `PersonController.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Debt API Controller`** (6 nodes): `DebtController`, `.createDebt()`, `.deleteDebt()`, `.getAllDebts()`, `.updateDebt()`, `DebtController.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Global Error Responses`** (5 nodes): `GlobalExceptionHandler`, `.handleGeneralException()`, `.handleIllegalArgumentException()`, `.handleNotFound()`, `GlobalExceptionHandler.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `SMS Notification Service`** (5 nodes): `SmsService`, `.formatPhoneNumber()`, `.init()`, `.sendDebtNotification()`, `SmsService.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Device Contact Integration`** (4 nodes): `ContactService.js`, `getDeviceContacts()`, `normalizePhoneNumber()`, `requestContactPermission()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Security Configuration`** (4 nodes): `SecurityConfig`, `.SecurityConfig()`, `.securityFilterChain()`, `SecurityConfig.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Spring Boot Entry Point`** (3 nodes): `DebtSettlementApplication`, `.main()`, `DebtSettlementApplication.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Backend Unit Testing`** (3 nodes): `DebtSettlementApplicationTests`, `.contextLoads()`, `DebtSettlementApplicationTests.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Debt Creation DTO`** (2 nodes): `CreateDebtRequest`, `CreateDebtRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Person Creation DTO`** (2 nodes): `CreatePersonRequest`, `CreatePersonRequest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Settlement Response DTO`** (2 nodes): `SettlementResponse`, `SettlementResponse.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Debt Model Entity`** (2 nodes): `Debt`, `Debt.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Person Model Entity`** (2 nodes): `Person`, `Person.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Package Name Fix`** (1 nodes): `Package Name Correction`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PersonController` connect `Person API Controller` to `User Profile Management`, `Person & Debt Persistence`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Why does `DebtService` connect `Person & Debt Persistence` to `Transaction Queries`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **What connects `CreateDebtRequest`, `CreatePersonRequest`, `SettlementResponse` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._