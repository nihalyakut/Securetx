# SecureTX

SecureTX is a Spring Boot based money transfer system designed with **financial safety**, **concurrency correctness**, and **auditability** as first-class concerns.

The project focuses on solving the hardest problems in transaction systems:
- concurrent balance updates
- idempotent request handling
- account freezing
- strong consistency under failure

All critical scenarios are verified using **real PostgreSQL** with **Testcontainers**.

---

## Key Capabilities

- **Concurrency-safe transfers**
- **Idempotency-key based request handling**
- **Account freeze / unfreeze**
- **Double-entry ledger accounting**
- **Full audit logging**
- **PostgreSQL row-level locking**
- **Integration tests with real database**

## Why This Exists

This project is designed to demonstrate how to safely move money in a distributed system.

Naive implementations fail under concurrency: two requests can spend the same balance at the same time.
SecureTX solves this using database-level locks, idempotency keys and strict transaction boundaries.

