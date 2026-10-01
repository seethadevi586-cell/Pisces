# Deployment & Infrastructure Architecture

## 1. Cloud & Container Architecture
- **Stateless Microservices**: The Copy Engine and API services can be horizontally scaled in AWS ECS / EKS or Google Cloud Run.
- **Persistent Data Tier**: PostgreSQL with read replicas for historical analytics, and Redis / BullMQ for distributed idempotent execution queues.
- **Encryption**: Secrets stored using AWS KMS / HashiCorp Vault. At-rest encryption using AES-256; in-transit TLS 1.3.

## 2. Low-Latency Execution Infrastructure
- For production Indian markets, deployment is recommended within Mumbai data centers (AWS `ap-south-1`) close to NSE/BSE colocation endpoints.
- Dedicated static IPs and leased line connectivity where required by exchange broker APIs.
