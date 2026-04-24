# 💸 Clear Dues

A production-grade Spring Boot REST API built to track and settle shared expenses within groups. Clear Dues uses a greedy graph algorithm to minimize the total number of transactions required to settle debts, backed by a stateless OAuth2 + JWT security architecture.

## ✨ Core Features

* **Smart Debt Settlement Algorithm:** Transforms a chaotic web of overlapping group debts into the absolute minimum number of transactions needed to settle everyone's balance. Time complexity is mathematically optimized to $O(N + E)$.
* **Graph Segregation (Scale-Ready):** Implements group-based isolation, preventing $O(N)$ memory crashes by only loading and processing localized clusters of users rather than the entire database.
* **Stateless Security (Mobile-Ready):** Replaces default Spring session cookies with a custom Google OAuth2 to JWT bridge, making the API fully stateless and perfect for mobile integrations (React Native / Flutter).
* **High-Performance SQL Aggregation:** Leverages custom JPQL queries to compute net balances directly at the database layer, drastically reducing JVM memory overhead.

## 🛠️ Tech Stack

* **Language:** Java 17+
* **Framework:** Spring Boot 3.x
* **Security:** Spring Security, OAuth2 Client, JSON Web Tokens (JJWT 0.12.x)
* **Database:** MySQL, Spring Data JPA, Hibernate
* **Tools:** Maven, Postman

## 🔐 Authentication Flow (OAuth2 + JWT Bridge)

This backend implements a custom authentication bridge designed for native mobile applications:
1. Mobile app redirects the user to the Spring Boot Google OAuth2 endpoint.
2. User authenticates with Google.
3. Spring Security intercepts the success response and provisions a new user in the MySQL database.
4. The `JwtService` mathematically signs a stateless JWT.
5. A custom `OAuth2LoginSuccessHandler` builds a deep link (`cleardues://login-success?token=...`) and redirects the user back to the mobile app with the token securely attached.

## 🚀 Getting Started

### Prerequisites
* Java 17 or higher
* MySQL running locally or via Docker
* A Google Cloud Console project with OAuth2 Credentials

### 1. Clone the Repository
```bash
git clone [https://github.com/YourUsername/fairshare-backend.git](https://github.com/YourUsername/fairshare-backend.git)
cd fairshare-backend
