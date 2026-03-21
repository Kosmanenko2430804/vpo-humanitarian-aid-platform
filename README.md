# ВПО Платформа допомоги

Вебзастосунок для координації гуманітарної допомоги внутрішньо переміщеним особам (ВПО) в Україні. Платформа з'єднує ВПО з волонтерами та організаціями, що надають допомогу.

## Функціонал

### Для ВПО
- Перегляд оголошень про допомогу з фільтрацією за категорією та містом
- Подача заявок на отримання допомоги з повідомленням
- Розміщення власних оголошень про потребу у допомозі
- Фінансова підтримка через LiqPay
- Особистий кабінет: оголошення, заявки, сповіщення, профіль

### Для волонтерів та організацій
- Розміщення оголошень про надання допомоги
- Прийом та відхилення заявок від ВПО із зазначенням дати, місця та телефону для отримання допомоги
- Позначення заявок як завершених після отримання ВПО допомоги
- Публічний профіль у каталозі волонтерів

### Для адміністратора
- Модерація оголошень (затвердження / відхилення з причиною)
- Розгляд скарг на оголошення
- Управління користувачами (блокування)

### Загальне
- Реєстрація та вхід через email або Google OAuth2
- Email-сповіщення про ключові події (Observer pattern)
- Внутрішні сповіщення в особистому кабінеті
- Система рейтингів та відгуків

## Технічний стек

| Шар | Технологія |
|-----|-----------|
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA |
| Frontend | Thymeleaf, Bootstrap 5, HTMX |
| База даних | PostgreSQL |
| Автентифікація | Spring Security + Google OAuth2 |
| Email | Spring Mail (SMTP) |
| Оплата | LiqPay API |
| Збірка | Maven, Lombok |

## Запуск локально

### Вимоги
- Java 21+
- Maven 3.9+
- PostgreSQL 14+

### 1. Клонувати репозиторій

```bash
git clone https://github.com/Kosmanenko2430804/vpo-humanitarian-aid-platform.git
cd vpo-humanitarian-aid-platform
```

### 2. Створити базу даних

```sql
CREATE DATABASE database_name;
```

### 3. Налаштувати змінні середовища

Створити файл `.env` у корені проекту:

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=

LIQPAY_PUBLIC_KEY=
LIQPAY_PRIVATE_KEY=

APP_BASE_URL=http://localhost:8080
PORT=8080
```

### 4. Запустити застосунок

```bash
./mvnw spring-boot:run
```

Застосунок буде доступний за адресою: `http://localhost:8080`

## Структура проекту

```
src/main/java/.../
├── controller/       # MVC контролери
├── service/          # Бізнес-логіка
├── repository/       # Spring Data JPA репозиторії
├── model/            # JPA сутності
├── enums/            # Ролі, статуси
├── event/            # Spring Events (Observer)
├── config/           # Spring Security, OAuth2
└── dto/              # DTO об'єкти

src/main/resources/
├── templates/        # Thymeleaf шаблони
│   ├── layout/       
│   ├── announcements/
│   ├── cabinet/
│   ├── admin/
│   ├── catalog/
│   └── payment/
└── static/
    ├── css/
    └── js/
```

## Ролі користувачів

| Роль | Опис |
|------|------|
| `VPO` | Внутрішньо переміщена особа |
| `PROVIDER` | Волонтер або організація |
| `ADMIN` | Адміністратор платформи |

## Тестування

Проект використовує JaCoCo для вимірювання покриття коду. Мінімальне покриття для пакету `service` — **70%**.

```bash
./mvnw test
```

Звіт про покриття генерується у `target/site/jacoco/index.html`.

## Ліцензія

Дивись [LICENSE](LICENSE).
