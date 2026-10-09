# LoginSeguro

Sistema de autenticação com **cadastro em duas etapas (2FA por e-mail)** e **gerenciamento de usuários com controle de permissões**, construído com Spring Boot, MongoDB, Thymeleaf, Spring Security e Tailwind CSS.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Fluxo de cadastro com 2FA](#fluxo-de-cadastro-com-2fa)
- [Gerenciamento de usuários e permissões](#gerenciamento-de-usuários-e-permissões)
- [Notifier de e-mail](#notifier-de-e-mail)
- [Endpoints](#endpoints)
- [Modelo de dados](#modelo-de-dados)
- [Arquitetura e estrutura de pastas](#arquitetura-e-estrutura-de-pastas)
- [Segurança](#segurança)
- [Configurações de cadastro](#configurações-de-cadastro)

---

## Funcionalidades

- **Cadastro com verificação em duas etapas (2FA) por e-mail**: o usuário só é criado após confirmar um código de 6 dígitos enviado ao e-mail informado.
- **Papéis de usuário**: `ADMIN`, `MANAGER` e `USER`. Todo usuário que se registra entra automaticamente como `USER`.
- **Gerenciamento de usuários na página inicial (`/`)**:
  - `ADMIN` pode **editar o nome** e **excluir** usuários.
  - `MANAGER` e `USER` têm acesso **somente leitura**.
- **Notifier de e-mail** desacoplado, com implementações de **console** (desenvolvimento) e **SMTP** (produção).
- **Índices únicos** de `username` e `email` no MongoDB.
- Proteções de segurança: senha com BCrypt, código 2FA com hash, expiração, cooldown de reenvio e limite de tentativas; proteção CSRF; autorização em duas camadas (UI + servidor).

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / Build | Java 17+ (projeto configurado para Java 17; testado no Java 21) e Maven (wrapper incluso) |
| Framework | Spring Boot 4.1.1 |
| Persistência | Spring Data MongoDB (MongoDB Atlas ou local) |
| View | Thymeleaf + Tailwind CSS (via CDN) |
| E-mail | Jakarta Mail API + Angus Mail (SMTP) |
| Utilitários | Lombok |

---

## Arquitetura e estrutura de pastas

O código segue uma organização em camadas, com a lógica de negócio isolada nos services e o envio de e-mail atrás de uma interface (porta/adapter).

```
src/main/java/com/umc/loginseguro/
├── LoginseguroApplication.java
├── config/
│   ├── SecurityConfig.java          # Regras de autorização, login/logout, encoder
│   └── DataInitializer.java         # Cria/atualiza o admin na inicialização
├── controller/
│   └── HomeController.java          # Rotas web (cadastro, 2FA, gerenciamento)
├── entity/
│   ├── User.java
│   ├── UserRole.java
│   └── PendingRegistration.java
├── notification/
│   ├── EmailNotifier.java           # Interface (porta) de envio
│   ├── ConsoleEmailNotifier.java    # Implementação de desenvolvimento
│   ├── SmtpEmailNotifier.java       # Implementação SMTP
│   └── EmailNotificationException.java
├── repository/
│   ├── UserRepository.java
│   └── PendingRegistrationRepository.java
└── service/
    ├── SystemUserDetailsService.java # Integração com o Spring Security
    ├── UserService.java              # Regras de gerenciamento de usuários
    └── RegistrationService.java      # Fluxo de cadastro + 2FA

src/main/resources/
├── application.properties
└── templates/
    ├── index.html        # Gerenciamento de usuários
    ├── login.html
    ├── registrar.html
    ├── verificar.html    # Confirmação do código 2FA
    └── fragments/
        └── input.html    # Fragmento reutilizável de campo
```

---

## Pré-requisitos

- Java 17 ou superior.
- Acesso a um banco MongoDB (local ou Atlas).
- Não é necessário instalar o Maven — use o wrapper `./mvnw`.

---

## Como rodar

1. Clone o repositório e entre na pasta:

   ```bash
   git clone <url-do-repositorio>
   cd LoginSeguro
   ```

2. Crie o arquivo `.env` a partir do exemplo e preencha os valores:

   ```bash
   cp .env.example .env
   ```

   No mínimo, configure `MONGODB_URI` e as credenciais do admin (`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`).

3. Suba a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Acesse: [http://localhost:8080](http://localhost:8080)

> Na primeira execução, o `DataInitializer` cria o usuário admin com as credenciais do `.env`, e os índices únicos são criados automaticamente.

---

## Variáveis de ambiente

O projeto usa o `springboot4-dotenv`, que carrega o arquivo `.env`.

### Banco e admin

| Variável | Descrição | Padrão |
|---|---|---|
| `MONGODB_URI` | String de conexão do MongoDB | — (obrigatória) |
| `ADMIN_USERNAME` | Nome do usuário administrador criado na inicialização | — |
| `ADMIN_PASSWORD` | Senha do administrador | — |
| `ADMIN_EMAIL` | E-mail do administrador | `admin@loginseguro.local` |

### E-mail / 2FA

| Variável | Descrição | Padrão |
|---|---|---|
| `MAIL_MODE` | `console` (grava o código no log) ou `smtp` (envio real) | `console` |
| `MAIL_FROM` | Remetente dos e-mails | `no-reply@loginseguro.local` |
| `MAIL_HOST` | Host SMTP (usado quando `MAIL_MODE=smtp`) | — |
| `MAIL_PORT` | Porta SMTP | `587` |
| `MAIL_USERNAME` | Usuário SMTP | — |
| `MAIL_PASSWORD` | Senha SMTP | — |

---

Regras aplicadas:

- Código de **6 dígitos**, armazenado apenas como **hash BCrypt**.
- **Expiração** padrão de 10 minutos.
- **Cooldown** de 60 segundos entre reenvios.
- **Limite** de 5 tentativas inválidas (depois disso o cadastro pendente é descartado).
- Unicidade de `username` e `email` verificada antes do envio e novamente na confirmação.

No modo `console`, o código aparece no log da aplicação, por exemplo:

```
========== E-MAIL SIMULADO (app.mail.mode=console) ==========
 Para:   usuario@exemplo.com
 Código: 512497
=============================================================
```

---

Para habilitar o envio real, ajuste o `.env`:

```env
MAIL_MODE=smtp
MAIL_FROM=no-reply@seudominio.com
MAIL_HOST=smtp.seudominio.com
MAIL_PORT=587
MAIL_USERNAME=seu-usuario
MAIL_PASSWORD=sua-senha
```
