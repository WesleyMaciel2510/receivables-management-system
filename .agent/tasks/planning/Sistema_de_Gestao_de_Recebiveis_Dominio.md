# Sistema de Gestão de Recebíveis --- Domínio de Negócio

## Objetivo

Este projeto simula um sistema de gestão de recebíveis para pequenas
empresas brasileiras, permitindo registrar vendas, acompanhar
pagamentos, controlar inadimplência e manter histórico financeiro.

## Fluxo do sistema

1.  Cadastro do cliente.
2.  Criação do recebível.
3.  Registro de pagamentos.
4.  Atualização automática do saldo.
5.  Marcação como vencido.
6.  Armazenamento de comprovantes.
7.  Histórico de transações.

## Exemplo

Cliente: João Silva

Venda: Desenvolvimento de Site

Valor: R\$2.500

Vencimento: 15/09/2026

Status inicial: PENDING

PIX de R\$500 → PARTIAL

PIX de R\$2.000 → PAID

## Entidades

### Cliente

-   id (UUID)
-   nome
-   cpfCnpj
-   email
-   telefone
-   status
-   createdAt

### Recebível

-   id
-   clientId
-   description
-   originalAmount
-   remainingBalance
-   dueDate
-   status
-   createdAt

Status possíveis:

-   PENDING
-   PARTIAL
-   PAID
-   OVERDUE
-   CANCELLED

### Pagamento

-   id
-   receivableId
-   amount
-   paymentMethod
-   paymentDate
-   transactionReference
-   notes

### DocumentoComprovante

-   id
-   receivableId
-   fileName
-   s3Key
-   uploadedAt

### Transaction (Audit Log)

-   id
-   receivableId
-   eventType
-   amount
-   createdAt

Eventos:

-   RECEIVABLE_CREATED
-   PAYMENT_RECEIVED
-   CANCELLATION
-   MANUAL_ADJUSTMENT

## Métodos de pagamento

-   PIX
-   Cartão de Crédito
-   Cartão de Débito
-   Boleto Bancário
-   Dinheiro
-   TED
-   Carteira Digital

## Regras de negócio

### Pagamento parcial

Recebível: R\$2.000

Pagamento: R\$500

Resultado:

-   Saldo: R\$1.500
-   Status: PARTIAL

### Pagamento total

Saldo chega a zero.

Sistema:

-   Status = PAID
-   Registra Transaction
-   Publica evento para processamento assíncrono.

### Recebível vencido

Se hoje for maior que dueDate:

-   Status = OVERDUE
-   Evento de cobrança é gerado.

## Dashboard

KPIs:

-   Total de Recebíveis
-   Valor Pendente
-   Valor Recebido
-   Valor Vencido
-   Taxa de Recebimento

Gráficos:

-   Recebimentos por mês
-   Métodos de pagamento
-   Evolução da inadimplência
-   Top clientes
