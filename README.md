# 💸 Settlement

A production-grade Spring Boot REST API built to track and settle shared expenses within groups. Settlement uses a greedy graph algorithm to minimize the total number of transactions required to settle debts, backed by a stateless OAuth2 + JWT security architecture.

## ✨ Core Features

* **Smart Debt Settlement Algorithm:** Transforms a chaotic web of overlapping group debts into the absolute minimum number of transactions needed to settle everyone's balance. Time complexity is mathematically optimized to $O(N + E)$.
* **Graph Segregation (Scale-Ready):** Implements group-based isolation, preventing $O(N)$ memory crashes by only loading and processing localized clusters of users rather than the entire database.
* **Stateless Security (Mobile-Ready):** Replaces default Spring session cookies with a custom Google OAuth2 to JWT bridge, making the API fully stateless and perfect for mobile integrations (React Native / Flutter).
* **High-Performance SQL Aggregation:** Leverages custom JPQL queries to compute net balances directly at the database layer, drastically reducing JVM memory overhead.

### Backend
* **Language:** Java 17+
* **Framework:** Spring Boot 3.x
* **Security:** Spring Security, OAuth2 Client, JSON Web Tokens (JJWT 0.12.x)
* **Database:** MySQL, Spring Data JPA, Hibernate

### Frontend (Mobile App)
* **Framework:** React Native with Expo (SDK 54) for cross-platform Android/iOS support.
* **Navigation:** Implements `React Navigation 7.x` with a fluid Bottom Tab layout and nested Stack Navigators for a seamless user flow.
* **State Management:** Powered by `Zustand` for lightweight, fast, and persistent global state management (handling auth tokens and user data).
* **UI & Aesthetics:** Features a modern **Glassmorphism Design System** with custom blurring, transparency, and a curated dark-theme palette.
* **Data Visualization:** Integrated `React Native SVG Charts` to provide visual representations of debt distributions and settlement progress.
* **Security:** Uses `Expo Secure Store` to encrypt and store JWT tokens locally on the device's keychain.
* **Networking:** Optimized `Axios` instance with request/response interceptors to handle automatic token attachment and 401 (Unauthorized) redirects.

## 🔐 Authentication Flow (OAuth2 + JWT Bridge)

This backend implements a custom authentication bridge designed for native mobile applications:
1. Mobile app redirects the user to the Spring Boot Google OAuth2 endpoint.
2. User authenticates with Google.
3. Spring Security intercepts the success response and provisions a new user in the MySQL database.
4. The `JwtService` mathematically signs a stateless JWT.
5. A custom `OAuth2LoginSuccessHandler` builds a deep link (`cleardues://login-success?token=...`) and redirects the user back to the mobile app with the token securely attached.

## 🚀 Deployment
### Backend (Railway)
This project is configured for one-click deployment on Railway:
1. Provision a **MySQL** instance.
2. Connect your GitHub repository.
3. Map environment variables (JWT_SECRET, GOOGLE_CLIENT_ID, etc.).
4. The API will be live on a public `.up.railway.app` domain.

### Frontend (EAS Build)
Generate the production APK using Expo Application Services:
1. Install EAS CLI: `npm install -g eas-cli`.
2. Configure build: `eas build:configure`.
3. Generate APK: `eas build -p android --profile preview`.

---

## 🛠️ Local Development
### Prerequisites
* Java 17 or higher
* MySQL running locally or via Docker
* A Google Cloud Console project with OAuth2 Credentials

### 1. Clone the Repository
```bash
git clone https://github.com/Krishna-110/Clear-Dues.git
cd Clear-Dues
```
