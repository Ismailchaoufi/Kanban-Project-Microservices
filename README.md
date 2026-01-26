# 🗂️ Kanban Project - Microservices Architecture

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen?style=for-the-badge&logo=spring-boot)
![Angular](https://img.shields.io/badge/Angular-19-red?style=for-the-badge&logo=angular)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker)
![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)

A full-stack Kanban board application built with microservices architecture, enabling teams to manage projects and tasks efficiently with drag-and-drop functionality.

[Features](#-features) • [Architecture](#-architecture) • [Getting Started](#-getting-started) • [API Documentation](#-api-endpoints) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running with Docker](#running-with-docker)
  - [Running Locally](#running-locally)
- [Services Overview](#-services-overview)
- [API Endpoints](#-api-endpoints)
- [Environment Variables](#-environment-variables)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### Core Features
- 🔐 **User Authentication** - Secure JWT-based authentication and authorization
- 📊 **Project Management** - Create, update, and manage multiple projects
- ✅ **Task Management** - Full CRUD operations for tasks with status tracking
- 📋 **Kanban Board** - Visual drag-and-drop task management interface
- 👥 **Team Collaboration** - Invite members to projects via email
- 📈 **Dashboard & Analytics** - Project statistics and task insights

### Technical Features
- 🏗️ **Microservices Architecture** - Scalable and maintainable service-oriented design
- 🔄 **Service Discovery** - Netflix Eureka for dynamic service registration
- ⚙️ **Centralized Configuration** - Spring Cloud Config Server
- 🚪 **API Gateway** - Spring Cloud Gateway for routing and filtering
- 🐳 **Docker Support** - Complete containerization with Docker Compose
- 🎨 **Modern UI** - Angular 19 with TailwindCSS and Angular Material

---

## 🏗 Architecture

```
                                    ┌─────────────────┐
                                    │   Config Server │
                                    │     :8888       │
                                    └────────┬────────┘
                                             │
                                    ┌────────▼────────┐
                                    │  Eureka Server  │
                                    │     :8761       │
                                    └────────┬────────┘
                                             │
┌──────────────┐              ┌──────────────▼──────────────┐
│   Frontend   │◄────────────►│       API Gateway           │
│  (Angular)   │              │          :8222              │
│    :80       │              └──────────────┬──────────────┘
└──────────────┘                             │
                         ┌───────────────────┼───────────────────┐
                         │                   │                   │
                ┌────────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐
                │  Auth Service   │ │ Project Service │ │  Task Service   │
                │     :8081       │ │      :8082      │ │     :8083       │
                └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
                         │                   │                   │
                ┌────────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐
                │    Auth DB      │ │   Project DB    │ │    Task DB      │
                │  (PostgreSQL)   │ │  (PostgreSQL)   │ │  (PostgreSQL)   │
                └─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## 🛠 Tech Stack

### Backend
| Technology | Description |
|------------|-------------|
| **Spring Boot 3.2.x** | Application framework |
| **Spring Cloud 2023.0.x** | Microservices toolkit |
| **Spring Security** | Authentication & Authorization |
| **Spring Data JPA** | Data persistence |
| **Netflix Eureka** | Service discovery |
| **Spring Cloud Gateway** | API Gateway |
| **Spring Cloud Config** | Centralized configuration |
| **PostgreSQL 15** | Relational database |
| **JWT (jjwt 0.11.5)** | Token-based authentication |
| **Lombok** | Boilerplate code reduction |

### Frontend
| Technology | Description |
|------------|-------------|
| **Angular 19** | Frontend framework |
| **TailwindCSS 3.4** | Utility-first CSS framework |
| **Angular Material 19** | UI component library |
| **Chart.js** | Data visualization |
| **Lucide Angular** | Icon library |
| **RxJS** | Reactive programming |

### DevOps
| Technology | Description |
|------------|-------------|
| **Docker** | Containerization |
| **Docker Compose** | Multi-container orchestration |
| **Nginx** | Frontend web server |

---

## 📁 Project Structure

```
kanban-microservices/
├── 📁 api-gateway/           # API Gateway service
├── 📁 auth-service/          # Authentication service
├── 📁 config-repo/           # Configuration files for all services
├── 📁 config-server/         # Spring Cloud Config Server
├── 📁 eureka-server/         # Service Discovery Server
├── 📁 kanban-frontend/       # Angular frontend application
├── 📁 project-service/       # Project management service
├── 📁 task-service/          # Task management service
├── 📄 docker-compose.yml     # Development Docker setup
├── 📄 docker-compose.prod.yml# Production Docker setup
└── 📄 README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Node.js 18+** and **npm**
- **Docker** and **Docker Compose**
- **Maven 3.8+**

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Ismailchaoufi/Kanban-Project-Microservices.git
   cd kanban-microservices
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

### Running with Docker

The easiest way to run the entire application:

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

**Service URLs after startup:**
| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| API Gateway | http://localhost:8222 |
| Eureka Dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Auth Service | http://localhost:8081 |
| Project Service | http://localhost:8082 |
| Task Service | http://localhost:8083 |

### Running Locally (Development)

1. **Start infrastructure services first:**
   ```bash
   # Start PostgreSQL databases
   docker-compose up -d auth-db project-db task-db
   ```

2. **Start Config Server:**
   ```bash
   cd config-server
   ./mvnw spring-boot:run
   ```

3. **Start Eureka Server:**
   ```bash
   cd eureka-server
   ./mvnw spring-boot:run
   ```

4. **Start microservices (in separate terminals):**
   ```bash
   # Auth Service
   cd auth-service
   ./mvnw spring-boot:run

   # Project Service
   cd project-service
   ./mvnw spring-boot:run

   # Task Service
   cd task-service
   ./mvnw spring-boot:run

   # API Gateway
   cd api-gateway
   ./mvnw spring-boot:run
   ```

5. **Start Frontend:**
   ```bash
   cd kanban-frontend
   npm install
   npm start
   ```

---

## 📦 Services Overview

### Config Server (Port: 8888)
Centralized configuration management for all microservices. Stores configuration in the `config-repo` directory.

### Eureka Server (Port: 8761)
Service discovery server that allows microservices to find and communicate with each other dynamically.

### API Gateway (Port: 8222)
Central entry point for all client requests. Handles:
- Request routing to appropriate services
- JWT token validation
- CORS configuration
- Load balancing

### Auth Service (Port: 8081)
Manages user authentication and authorization:
- User registration and login
- JWT token generation
- User management
- Role-based access control

### Project Service (Port: 8082)
Handles project management operations:
- CRUD operations for projects
- Member management
- Email invitations
- Project statistics

### Task Service (Port: 8083)
Manages tasks within projects:
- CRUD operations for tasks
- Task status management (TODO, IN_PROGRESS, DONE)
- Task statistics

### Frontend (Port: 80)
Modern Angular application with:
- Responsive Kanban board interface
- Drag-and-drop task management
- User authentication flows
- Project and team management
- Dashboard with charts

---

## 🔌 API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register a new user |
| POST | `/login` | Authenticate user |
| GET | `/users` | Get all users |
| GET | `/users/search` | Search users |

### Projects (`/api/v1/projects`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all projects |
| GET | `/{id}` | Get project by ID |
| POST | `/` | Create new project |
| PUT | `/{id}` | Update project |
| DELETE | `/{id}` | Delete project |
| GET | `/{id}/members` | Get project members |
| POST | `/{id}/members` | Add project member |
| DELETE | `/{id}/members/{userId}` | Remove member |
| POST | `/{id}/invite` | Send invitation email |
| GET | `/invitations/verify` | Verify invitation token |
| POST | `/invitations/accept` | Accept invitation |

### Tasks (`/api/v1/tasks`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get tasks (with filters) |
| GET | `/{id}` | Get task by ID |
| POST | `/` | Create new task |
| PUT | `/{id}` | Update task |
| PATCH | `/{id}/status` | Update task status |
| DELETE | `/{id}` | Delete task |
| GET | `/stats` | Get task statistics |

---

## ⚙️ Environment Variables

### Docker Compose Environment
```env
# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key

# Database Credentials
AUTH_DB_USER=auth_user
AUTH_DB_PASSWORD=auth_pass
PROJECT_DB_USER=project_user
PROJECT_DB_PASSWORD=project_pass
TASK_DB_USER=task_user
TASK_DB_PASSWORD=task_pass

# Email Configuration (for invitations)
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password

# Frontend URL
APP_FRONTEND_URL=http://localhost
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines
- Follow existing code style and conventions
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 👨‍💻 Author

**Ismail Chaoufi**

- GitHub: [@Ismailchaoufi](https://github.com/Ismailchaoufi)
- GitLab: [@ichaoufi](https://gitlab.com/ichaoufi)
