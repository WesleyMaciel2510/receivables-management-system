# Sistema de Gestão de Recebíveis --- Arquitetura, Banco de Dados e APIs

## Banco de Dados (PostgreSQL)

Relacionamentos:

-   Cliente 1:N Recebível
-   Recebível 1:N Pagamento
-   Recebível 1:N Transaction
-   Recebível 1:1 DocumentoComprovante

## Estrutura das tabelas

### clients

-   id (UUID)
-   name
-   cpf_cnpj
-   email
-   phone
-   status
-   created_at

### receivables

-   id
-   client_id
-   description
-   original_amount
-   remaining_balance
-   due_date
-   status
-   created_at

### payments

-   id
-   receivable_id
-   amount
-   payment_method
-   payment_date
-   transaction_reference
-   notes

### documents

-   id
-   receivable_id
-   file_name
-   s3_key
-   uploaded_at

### transactions

-   id
-   receivable_id
-   event_type
-   amount
-   created_at

## CRUDs

### Cliente

-   POST /clients
-   GET /clients
-   GET /clients/{id}
-   PUT /clients/{id}
-   DELETE /clients/{id}

### Recebível

-   POST /receivables
-   GET /receivables
-   GET /receivables/{id}
-   PUT /receivables/{id}
-   PATCH /receivables/{id}/status
-   DELETE /receivables/{id}

Exemplo:

``` json
{
  "clientId": "123",
  "description": "Website Development",
  "amount": 2500,
  "dueDate": "2026-09-15"
}
```

### Pagamento

-   POST /payments
-   GET /payments
-   GET /payments/{id}

Pagamentos não devem ser editados ou apagados.

Use:

-   Refund
-   Reversal
-   Adjustment

## Enum PaymentMethod

``` java
public enum PaymentMethod {
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    BOLETO,
    CASH,
    TED,
    DIGITAL_WALLET
}
```

## Integração com AWS

  Recurso        Uso
  -------------- ---------------------
  S3             Comprovantes
  SQS            Reconciliação
  Spring Batch   Recebíveis vencidos
  CloudWatch     Logs
  API Gateway    API
  Lambda         Futuro deployment

## Roadmap técnico

1.  CRUD Clientes
2.  CRUD Recebíveis
3.  Registro de Pagamentos
4.  Audit Log
5.  Upload S3
6.  Batch de inadimplência
7.  SQS
8.  Dashboard

## Diferenciais para portfólio

-   Java 21
-   Spring Boot
-   PostgreSQL
-   Flyway
-   Spring Security
-   JWT
-   S3
-   SQS
-   Spring Batch
-   Arquitetura em camadas
-   Regras reais do mercado brasileiro
