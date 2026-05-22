---
type: "query"
date: "2026-05-22T07:10:55.110003+00:00"
question: "Why does PersonController connect Person API Controller to User Profile Management, Person & Debt Persistence"
contributor: "graphify"
source_nodes: ["PersonController", "PersonRepository"]
---

# Q: Why does PersonController connect Person API Controller to User Profile Management, Person & Debt Persistence

## Answer

PersonController acts as a vital bridge because it handles incoming HTTP endpoints for contacts and profile retrieval. Through .getProfile(), it invokes PersonRepository.findByEmail(), which maps directly to User Profile Management (e.g., ProfileController and OAuth2LoginSuccessHandler). Simultaneously, through .checkPersonExists(), it invokes PersonRepository.findByPhoneNumber(), which links straight to Person & Debt Persistence services like DebtService.createDebt() and SettlementService.completeSettlement() to resolve, normalise, and update group transactions.

## Source Nodes

- PersonController
- PersonRepository