# BrewMaster — Application Specification

> **Version:** 1.0.0  
> **Target Audience:** Claude Code  
> **Status:** Initial Specification

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture Overview](#3-architecture-overview)
4. [Project Structure](#4-project-structure)
5. [Database Schema](#5-database-schema)
6. [Authentication & Authorization](#6-authentication--authorization)
7. [API Design](#7-api-design)
8. [Feature Specifications](#8-feature-specifications)
   - 8.1 [Brewing Calendar](#81-brewing-calendar)
   - 8.2 [AI Recipe Finder](#82-ai-recipe-finder)
   - 8.3 [Brew Mode (Step-by-Step)](#83-brew-mode-step-by-step)
   - 8.4 [Order & Price Finder](#84-order--price-finder)
9. [Frontend Design & UX](#9-frontend-design--ux)
10. [AI & External Service Integration](#10-ai--external-service-integration)
11. [Security Requirements](#11-security-requirements)
12. [Non-Functional Requirements](#12-non-functional-requirements)
13. [Development Guidelines for Claude Code](#13-development-guidelines-for-claude-code)
14. [Environment Configuration](#14-environment-configuration)
15. [Future Features (Backlog)](#15-future-features-backlog)

---

## 1. Project Overview

**BrewMaster** is a mobile-first web application for hobby beer brewers. It enables a group of friends to collaboratively plan brewing sessions, discover and manage beer recipes, follow guided step-by-step brew instructions, and source ingredients at the best available prices in Germany.

### Core Goals

- Provide a shared space for a brewing group to coordinate and plan.
- Leverage AI and real-time web search to assist with recipe discovery and ingredient sourcing.
- Guide users through the brewing process with precise, parameterized instructions.
- Be fully usable on mobile devices (phones and tablets).

---

## 2. Technology Stack

| Layer | Technology | Notes |
|---|---|---|
| **Frontend** | Angular 17+ (standalone components) | Responsive / mobile-first |
| **UI Component Library** | Angular Material (MDC) | Consistent, accessible components |
| **State Management** | NgRx (signals-based) or Angular Signals | Keep lightweight |
| **Backend** | Spring Boot 3.x (Java 21) | REST API |
| **Build Tool (Backend)** | Maven | Standard enterprise setup |
| **Database** | PostgreSQL 15+ | Primary data store |
| **ORM** | Spring Data JPA / Hibernate | |
| **Authentication** | Spring Security + JWT | Username/password auth |
| **AI Integration** | Anthropic Claude API (claude-sonnet-4-20250514) | Recipe finder & order feature |
| **Web Search** | Anthropic Web Search Tool (built-in to Claude API) | Used within AI features |
| **Containerization** | Docker + Docker Compose | Local development & deployment |
| **API Documentation** | SpringDoc OpenAPI (Swagger UI) | Auto-generated |

---

## 3. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (Browser / Mobile)             │
│                     Angular SPA (port 4200)                  │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTPS REST (JSON)
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot API Server (port 8080)         │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ Auth Module  │  │ Recipe Module│  │  Calendar Module │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│  ┌──────────────┐  ┌──────────────────────────────────────┐ │
│  │ Order Module │  │       AI Service (Claude API)        │ │
│  └──────────────┘  └──────────────────────────────────────┘ │
│                             │                                │
│               Spring Data JPA / Hibernate                    │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Database (port 5432)             │
└─────────────────────────────────────────────────────────────┘

External Services:
  • Anthropic API  →  AI text generation + web search
```

### Communication

- All API calls are authenticated via **JWT Bearer tokens**.
- The Angular frontend communicates with the backend exclusively through versioned REST endpoints (`/api/v1/...`).
- The backend calls the Anthropic API server-side; the API key is **never exposed to the frontend**.

---

## 4. Project Structure

```
brewmaster/
├── docker-compose.yml
├── .env.example
│
├── backend/                          # Spring Boot project root
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/brewmaster/
│       │   ├── BrewMasterApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtConfig.java
│       │   │   └── CorsConfig.java
│       │   ├── auth/
│       │   │   ├── AuthController.java
│       │   │   ├── AuthService.java
│       │   │   ├── JwtService.java
│       │   │   └── dto/
│       │   ├── user/
│       │   │   ├── User.java              (JPA Entity)
│       │   │   ├── UserRepository.java
│       │   │   ├── UserService.java
│       │   │   └── UserController.java
│       │   ├── calendar/
│       │   │   ├── BrewEvent.java
│       │   │   ├── BrewEventRepository.java
│       │   │   ├── BrewEventService.java
│       │   │   └── BrewEventController.java
│       │   ├── recipe/
│       │   │   ├── Recipe.java
│       │   │   ├── RecipeStep.java
│       │   │   ├── Ingredient.java
│       │   │   ├── RecipeRepository.java
│       │   │   ├── RecipeService.java
│       │   │   └── RecipeController.java
│       │   ├── brew/
│       │   │   ├── BrewSession.java
│       │   │   ├── BrewSessionRepository.java
│       │   │   ├── BrewSessionService.java
│       │   │   └── BrewSessionController.java
│       │   ├── order/
│       │   │   ├── OrderList.java
│       │   │   ├── OrderService.java
│       │   │   └── OrderController.java
│       │   └── ai/
│       │       ├── AnthropicClient.java
│       │       ├── RecipeAiService.java
│       │       └── OrderAiService.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── db/migration/           (Flyway migrations)
│               ├── V1__init_schema.sql
│               └── V2__seed_data.sql
│
└── frontend/                          # Angular project root
    ├── angular.json
    ├── package.json
    └── src/
        ├── app/
        │   ├── app.config.ts
        │   ├── app.routes.ts
        │   ├── core/
        │   │   ├── auth/
        │   │   │   ├── auth.service.ts
        │   │   │   ├── auth.guard.ts
        │   │   │   └── jwt.interceptor.ts
        │   │   ├── api/
        │   │   │   └── api.service.ts
        │   │   └── models/
        │   ├── features/
        │   │   ├── auth/
        │   │   │   ├── login/
        │   │   │   └── register/
        │   │   ├── dashboard/
        │   │   ├── calendar/
        │   │   ├── recipe-finder/
        │   │   ├── brew-mode/
        │   │   └── order/
        │   └── shared/
        │       ├── components/
        │       ├── directives/
        │       └── pipes/
        └── environments/
            ├── environment.ts
            └── environment.prod.ts
```

---

## 5. Database Schema

### Tables

#### `users`
```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,          -- bcrypt hash
    display_name VARCHAR(100),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

#### `brew_events` (Calendar)
```sql
CREATE TABLE brew_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    brew_date     DATE         NOT NULL,
    start_time    TIME,
    location      VARCHAR(200),
    recipe_id     UUID REFERENCES recipes(id) ON DELETE SET NULL,
    created_by    UUID         NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE brew_event_participants (
    event_id   UUID REFERENCES brew_events(id) ON DELETE CASCADE,
    user_id    UUID REFERENCES users(id) ON DELETE CASCADE,
    rsvp       VARCHAR(20) DEFAULT 'PENDING',   -- PENDING | ACCEPTED | DECLINED
    PRIMARY KEY (event_id, user_id)
);
```

#### `recipes`
```sql
CREATE TABLE recipes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200)  NOT NULL,
    style           VARCHAR(100),
    description     TEXT,
    source_url      TEXT,
    base_volume_l   DECIMAL(6,2)  NOT NULL DEFAULT 20,
    original_gravity DECIMAL(5,4),
    final_gravity   DECIMAL(5,4),
    abv             DECIMAL(4,2),
    ibu             INTEGER,
    srm             DECIMAL(5,2),
    mash_temp_c     DECIMAL(4,1),
    mash_duration_min INTEGER,
    boil_duration_min INTEGER,
    fermentation_temp_c DECIMAL(4,1),
    fermentation_days   INTEGER,
    notes           TEXT,
    is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

#### `recipe_ingredients`
```sql
CREATE TABLE recipe_ingredients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(50)  NOT NULL,  -- MALT | HOP | YEAST | ADJUNCT | WATER_TREATMENT | OTHER
    amount          DECIMAL(10,3) NOT NULL,
    unit            VARCHAR(20)  NOT NULL,  -- kg | g | l | ml | pcs | tsp | tbsp
    addition_time   VARCHAR(100),           -- e.g. "60 min before end of boil"
    notes           TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0
);
```

#### `recipe_steps`
```sql
CREATE TABLE recipe_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_number     INTEGER NOT NULL,
    phase           VARCHAR(50) NOT NULL,  -- PREPARATION | MASHING | LAUTERING | BOILING | COOLING | FERMENTATION | CONDITIONING | PACKAGING
    title           VARCHAR(200) NOT NULL,
    instructions    TEXT NOT NULL,
    duration_min    INTEGER,
    target_temp_c   DECIMAL(4,1),
    timer_required  BOOLEAN NOT NULL DEFAULT FALSE,
    notes           TEXT
);
```

#### `brew_sessions`
```sql
CREATE TABLE brew_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID REFERENCES recipes(id) ON DELETE SET NULL,
    event_id        UUID REFERENCES brew_events(id) ON DELETE SET NULL,
    volume_l        DECIMAL(6,2) NOT NULL,
    current_step    INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS | COMPLETED | ABANDONED
    notes           TEXT,
    started_by      UUID NOT NULL REFERENCES users(id),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE brew_session_step_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES brew_sessions(id) ON DELETE CASCADE,
    step_number     INTEGER NOT NULL,
    completed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actual_temp_c   DECIMAL(4,1),
    notes           TEXT
);
```

#### `order_lists`
```sql
CREATE TABLE order_lists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID NOT NULL REFERENCES recipes(id),
    volume_l        DECIMAL(6,2) NOT NULL,
    ai_result       JSONB,                  -- Full AI response with price data
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 6. Authentication & Authorization

### Mechanism

- **Spring Security** with stateless JWT-based authentication.
- Passwords stored as **bcrypt** hashes (strength 12).
- JWT tokens signed with HS256 (configurable secret via environment variable).
- Token expiry: **8 hours** (access token). No refresh token in v1.

### Endpoints (public)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Create new account |
| POST | `/api/v1/auth/login` | Authenticate, receive JWT |

### Roles

| Role | Description |
|---|---|
| `USER` | Standard brewer — all features |
| `ADMIN` | Can manage users (future) |

### Frontend

- Angular `AuthGuard` protects all routes except `/login` and `/register`.
- JWT is stored in `localStorage`.
- `JwtInterceptor` automatically adds `Authorization: Bearer <token>` to all HTTP requests.
- On 401 responses, the user is redirected to `/login`.

### DTOs

**Register Request:**
```json
{
  "username": "string (3-50 chars, alphanumeric + underscore)",
  "email": "string (valid email)",
  "password": "string (min 8 chars)",
  "displayName": "string (optional)"
}
```

**Login Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Auth Response:**
```json
{
  "token": "string (JWT)",
  "expiresIn": 28800,
  "user": {
    "id": "uuid",
    "username": "string",
    "displayName": "string",
    "role": "USER"
  }
}
```

---

## 7. API Design

### Conventions

- Base path: `/api/v1`
- Content-Type: `application/json`
- Authentication: `Authorization: Bearer <jwt>` header on all protected routes
- Error format:
```json
{
  "timestamp": "ISO-8601",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable description",
  "path": "/api/v1/..."
}
```
- Pagination: `?page=0&size=20&sort=createdAt,desc` (Spring Data Pageable)

### Endpoint Summary

#### Auth
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /api/v1/auth/me
```

#### Users
```
GET    /api/v1/users                  (ADMIN only)
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
```

#### Calendar / Brew Events
```
GET    /api/v1/events                 List all events (with optional ?month=YYYY-MM)
POST   /api/v1/events                 Create event
GET    /api/v1/events/{id}
PUT    /api/v1/events/{id}
DELETE /api/v1/events/{id}
POST   /api/v1/events/{id}/rsvp       Body: { "status": "ACCEPTED|DECLINED" }
```

#### Recipes
```
GET    /api/v1/recipes                List saved recipes
POST   /api/v1/recipes                Save a recipe
GET    /api/v1/recipes/{id}
PUT    /api/v1/recipes/{id}
DELETE /api/v1/recipes/{id}
POST   /api/v1/recipes/ai-search      AI recipe finder (see Feature 8.2)
POST   /api/v1/recipes/{id}/scale     Scale recipe to new volume
```

#### Brew Sessions (Brew Mode)
```
POST   /api/v1/sessions               Start a brew session
GET    /api/v1/sessions/{id}
PUT    /api/v1/sessions/{id}/step     Advance to next step
PUT    /api/v1/sessions/{id}/complete Mark session complete
DELETE /api/v1/sessions/{id}
```

#### Orders
```
POST   /api/v1/orders/generate        AI order generation (see Feature 8.4)
GET    /api/v1/orders                 List past order lists
GET    /api/v1/orders/{id}
```

---

## 8. Feature Specifications

### 8.1 Brewing Calendar

#### Description

A shared calendar view that lets all users see upcoming brewing sessions, create new events, and RSVP. The calendar is the app's home dashboard.

#### UI

- **Month view** as default (Angular Material or a lightweight calendar library such as `ngx-mat-calendar`).
- **List view** toggle for mobile friendliness.
- Events are color-coded:
  - Green: User has RSVP'd YES
  - Yellow: Pending invitation
  - Grey: User declined or event is past
- Clicking an event opens a detail sheet/modal showing: title, date, time, location, linked recipe, participant list with RSVP status.
- A **floating action button (FAB)** opens the "Create Event" form.

#### Create / Edit Event Form Fields

| Field | Type | Required |
|---|---|---|
| Title | Text | Yes |
| Date | Date picker | Yes |
| Start Time | Time picker | No |
| Location | Text | No |
| Linked Recipe | Recipe selector (dropdown/search) | No |
| Description | Textarea | No |
| Invite Users | Multi-user selector | No |

#### Backend Logic

- On event creation, all invited users receive an entry in `brew_event_participants` with status `PENDING`.
- The creator is automatically added with status `ACCEPTED`.
- `GET /api/v1/events?month=2025-04` returns all events in that month.

---

### 8.2 AI Recipe Finder

#### Description

The user describes the kind of beer they want to brew using taste profile parameters. The backend calls the Claude API with the web search tool enabled. Claude searches for fitting homebrew recipes on the internet and returns structured recipe suggestions that can be saved to the user's recipe library.

#### UI Flow

1. **Taste Profile Form** — User fills in preferences:
   - Beer Style (dropdown with common styles: IPA, Stout, Weizen, Lager, Saison, etc. + free text)
   - Bitterness Level (slider 1–5: Very Low → Very High)
   - Sweetness Level (slider 1–5)
   - Colour (dropdown: Pale / Amber / Red / Dark / Black)
   - Target ABV % (range slider: 0–12%)
   - Aroma notes (multi-chip input: Citrus, Pine, Fruity, Malty, Roasty, Spicy, Floral, Earthy, etc.)
   - Target batch volume (numeric, in litres)
   - Additional free-text notes (optional)
2. **"Find Recipes" button** triggers the API call. A loading spinner with the message *"Searching the web for recipes..."* is shown.
3. **Results view** shows 2–4 recipe cards, each with:
   - Recipe name & style
   - Short description
   - Key stats (OG, FG, ABV, IBU, SRM)
   - Source URL (if found via web search)
   - Ingredient count / overview
   - **"Save & Use This Recipe"** button
4. Selecting a recipe saves it to the database and optionally links it to a calendar event. User is then offered to go to **Brew Mode** directly.

#### Backend

**POST `/api/v1/recipes/ai-search`**

Request body:
```json
{
  "style": "IPA",
  "bitternessLevel": 4,
  "sweetnessLevel": 2,
  "colour": "PALE",
  "targetAbvMin": 5.5,
  "targetAbvMax": 7.0,
  "aromaNotes": ["citrus", "pine", "tropical"],
  "batchVolumeL": 20,
  "additionalNotes": "Something suitable for summer"
}
```

Response body:
```json
{
  "recipes": [
    {
      "name": "Pacific Summer IPA",
      "style": "American IPA",
      "description": "...",
      "sourceUrl": "https://...",
      "baseVolumeL": 20,
      "originalGravity": 1.062,
      "finalGravity": 1.012,
      "abv": 6.5,
      "ibu": 65,
      "srm": 6,
      "mashTempC": 66.5,
      "mashDurationMin": 60,
      "boilDurationMin": 60,
      "fermentationTempC": 19,
      "fermentationDays": 14,
      "ingredients": [
        {
          "name": "Pale Malt (2-Row)",
          "category": "MALT",
          "amount": 4.5,
          "unit": "kg",
          "notes": "Base malt"
        }
      ],
      "steps": [
        {
          "stepNumber": 1,
          "phase": "PREPARATION",
          "title": "Prepare strike water",
          "instructions": "Heat 15 litres of water to 72°C to achieve a mash temperature of 66.5°C.",
          "durationMin": 20,
          "targetTempC": 72,
          "timerRequired": false
        }
      ]
    }
  ]
}
```

#### Claude API Prompt Strategy

The backend constructs a prompt for Claude that:

1. Instructs it to act as a homebrewing expert.
2. Provides the taste profile in structured form.
3. Explicitly instructs it to use the web search tool to find 2–4 real homebrew recipes from reputable sources (e.g., homebrewtalk.com, brewersfriend.com, homebrewing.org, brulosophy.com).
4. Instructs it to return a **strict JSON response** conforming to the recipe schema above — no markdown, no prose.
5. Includes clear instructions for scaling all ingredients to the target batch volume.

The `AnthropicClient.java` service handles:
- Building the messages array with the system prompt and user message.
- Enabling `web_search` tool.
- Parsing the response (handling tool_use and text blocks).
- Extracting and deserializing the JSON.

---

### 8.3 Brew Mode (Step-by-Step)

#### Description

An interactive mode that guides the user through the brewing process. The recipe is scaled to the target volume and all steps are presented one at a time with precise, dynamic instructions.

#### Recipe Scaling

When a recipe is loaded into Brew Mode with a volume different from its base volume:

- All ingredient amounts are scaled linearly: `scaled_amount = base_amount × (target_volume / base_volume)`
- Temperatures remain unchanged.
- Strike water and sparge water volumes are recalculated using standard homebrewing formulae:
  - Strike water (L) = `grain_weight_kg × 3.0` (3:1 water-to-grain ratio by mass, adjustable)
  - Sparge volume = `target_pre_boil_volume - strike_water_volume`
- Pre-boil volume accounts for evaporation: `pre_boil_volume = target_volume / (1 - boil_off_rate)`, where `boil_off_rate` defaults to 10% per hour.
- All scaled values are rounded to 2 decimal places.

**POST `/api/v1/recipes/{id}/scale`**
```json
{
  "targetVolumeL": 25,
  "boilOffRatePercent": 10,
  "waterToGrainRatio": 3.0
}
```

#### UI Flow

1. **Setup Screen** — User selects a saved recipe and sets parameters:
   - Target batch volume (L)
   - Boil-off rate (%)
   - Water-to-grain ratio
   - Any additional notes
2. **"Start Brew"** button creates a `BrewSession` in the backend and transitions to step view.
3. **Step View** — Full-screen card showing:
   - Phase badge (e.g., "MASHING")
   - Step number (e.g., "Step 3 of 14")
   - Step title
   - Scaled instruction text (all ingredient amounts dynamically substituted)
   - Target temperature (if applicable)
   - Duration / Countdown timer (if `timerRequired` is true — starts on user tap)
   - Notes field (optional — user can log observations)
   - **"Complete Step"** button (logs to `brew_session_step_log`, advances `current_step`)
   - **"Previous Step"** button (goes back without un-logging)
4. **Phase Header** — When transitioning between phases (e.g., from MASHING to LAUTERING), display a full-screen phase transition card with a summary of what's coming.
5. **Completion Screen** — Summary of the session: recipe, volume, time elapsed, all completed steps with timestamps. Option to add final notes and mark as COMPLETE.

#### Timer Component

- Countdown timer implemented as an Angular component using `setInterval`.
- Alarm notification (browser notification API or audio cue) when timer reaches zero.
- Timer persists if user navigates away (kept in component state or a service).

#### Ingredient Display

Scaled ingredient amounts are substituted inline into instruction text using a template format. E.g.:
> *"Add **4.5 kg** of Pale Malt to the mash tun."*

The backend provides the instruction text with `{ingredient_id}` placeholders; the frontend replaces them with scaled amounts.

---

### 8.4 Order & Price Finder

#### Description

Based on a selected recipe and the target volume, this feature generates a complete shopping list and uses AI with web search to find the cheapest current offers for each ingredient in Germany.

#### UI Flow

1. User navigates to **Order** from a recipe or from the main menu.
2. Selects a recipe and sets target volume.
3. A **scaled ingredient list** is shown for review.
4. **"Find Best Prices"** button triggers the AI search. Loading state: *"Searching for the best prices in Germany..."*
5. **Results view** — A table/card list showing for each ingredient:
   - Ingredient name & required amount
   - Best price found (€)
   - Shop name & direct link to product page
   - Alternative shop (if found)
6. **Total estimated cost** shown at the bottom.
7. User can **export the order list** (copy to clipboard as formatted text, or download as PDF/CSV — v2 feature).
8. The result is automatically saved to `order_lists` table.

#### Backend

**POST `/api/v1/orders/generate`**

Request:
```json
{
  "recipeId": "uuid",
  "volumeL": 20
}
```

Response:
```json
{
  "orderId": "uuid",
  "recipeId": "uuid",
  "recipeName": "Pacific Summer IPA",
  "volumeL": 20,
  "items": [
    {
      "ingredientName": "Pale Malt (2-Row)",
      "requiredAmount": 4.5,
      "unit": "kg",
      "bestOffer": {
        "shopName": "Braupartner",
        "price": 3.99,
        "pricePerUnit": "€3.99/kg",
        "productUrl": "https://...",
        "packageSize": "5 kg",
        "packagesNeeded": 1,
        "totalCost": 3.99
      },
      "alternativeOffer": {
        "shopName": "Hobbybrauer Versand",
        "price": 4.49,
        "productUrl": "https://..."
      }
    }
  ],
  "estimatedTotalMin": 32.50,
  "estimatedTotalMax": 38.90,
  "generatedAt": "ISO-8601",
  "disclaimer": "Prices sourced via web search and may vary. Verify before ordering."
}
```

#### Claude API Prompt Strategy

The backend constructs a prompt that:

1. Provides the full scaled ingredient list in JSON format.
2. Instructs Claude to use the web search tool to find current prices for each ingredient in **German homebrewing online shops** (e.g., braupartner.de, hobbybrauer.de, maischemalzundmehr.de, brouwland.com, brewup.eu).
3. Instructs Claude to return a **strict JSON response** matching the schema above.
4. Includes fallback instruction: if a price cannot be found for an ingredient, include `"bestOffer": null` and note in a `"searchNote"` field.
5. Explicitly requests EUR pricing only.

---

## 9. Frontend Design & UX

### Design Principles

- **Mobile-first**: All layouts designed for 375px width upward. Desktop is an enhancement.
- **Bottom navigation bar** on mobile for primary navigation (max 4 items).
- **Toolbar / Sidenav** on desktop.
- Theme: Dark amber/copper tones reflecting brewing aesthetics. Use Angular Material theming.

### Color Palette

| Token | Value | Usage |
|---|---|---|
| Primary | `#C17817` (Amber) | Buttons, accents, active states |
| Secondary | `#3E2723` (Dark Brown) | Toolbar, sidebars |
| Background | `#1A1A1A` (Near-black) | App background |
| Surface | `#2C2C2C` (Dark grey) | Cards, inputs |
| On-Primary | `#FFFFFF` | Text on primary buttons |
| Success | `#4CAF50` | Completed steps, RSVP accepted |
| Warning | `#FFC107` | Pending states, timers |
| Error | `#F44336` | Errors, declines |

### Navigation Structure

```
Bottom Nav (mobile) / Sidenav (desktop):
  🏠 Dashboard / Calendar
  🔍 Recipe Finder
  🍺 My Recipes
  🛒 Order
```

### Responsive Breakpoints

| Breakpoint | Width | Layout |
|---|---|---|
| xs | < 600px | Single column, bottom nav |
| sm | 600–960px | Single column, bottom nav |
| md | 960–1280px | Two columns, side nav |
| lg | > 1280px | Two columns + content area, side nav |

### Key UX Considerations

- All long AI operations (recipe search, price finding) must show a **progress indicator with descriptive text**.
- Brew Mode step view should prevent accidental navigation away (browser `beforeunload` guard + in-app warning dialog).
- All forms validate inline with Angular reactive forms + Angular Material error messages.
- Offline-graceful: Show a clear error message if the backend is unreachable; no silent failures.

---

## 10. AI & External Service Integration

### Anthropic Client (`AnthropicClient.java`)

```java
// Core responsibility: Call Anthropic /v1/messages, handle tool_use blocks, extract text response.
// Configuration via application.yml:
//   anthropic.api-key: ${ANTHROPIC_API_KEY}
//   anthropic.model: claude-sonnet-4-20250514
//   anthropic.max-tokens: 4096
```

- All calls are made with the `web_search_20250305` tool enabled.
- Implement retry logic: 2 retries with exponential backoff on 529 (overload) responses.
- Implement a **timeout of 60 seconds** per call.
- Log all AI calls (request/response) at DEBUG level; never log the API key.
- Handle multi-block responses: concatenate all `text` type content blocks.

### JSON Extraction

Since Claude may occasionally wrap JSON in markdown code fences, implement a utility method that:
1. Tries `JSON.parse` directly.
2. Falls back to extracting content between ` ```json ` and ` ``` ` blocks.
3. Throws a descriptive exception if neither works.

---

## 11. Security Requirements

- **Passwords**: bcrypt hashed, never stored or logged in plaintext.
- **JWT secret**: Minimum 256-bit random secret, loaded from environment variable `JWT_SECRET`.
- **CORS**: Configured to allow only the frontend origin (`FRONTEND_URL` env var). No wildcard in production.
- **HTTPS**: Required in production. Backend should redirect HTTP → HTTPS.
- **Input validation**: All incoming DTOs validated with Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, etc.).
- **SQL Injection**: Not possible via JPA/Hibernate parameterized queries. No raw SQL with string concatenation.
- **Sensitive data**: `ANTHROPIC_API_KEY` loaded from environment variable only, never committed to source control.
- **Rate limiting**: Consider adding `bucket4j` Spring Boot starter to limit AI endpoints (e.g., max 10 AI requests per user per hour) to prevent abuse and cost overruns.

---

## 12. Non-Functional Requirements

| Requirement | Target |
|---|---|
| API response time (non-AI) | < 300ms P95 |
| AI endpoint response time | < 60s (with loading UI) |
| Mobile page load (initial) | < 3s on 4G |
| Angular bundle size | < 500KB gzipped |
| Database connection pool | Min 5, Max 20 connections (HikariCP) |
| Session timeout | 8 hours (JWT expiry) |
| Browser support | Chrome 115+, Firefox 115+, Safari 16+, Chrome Android |

---

## 13. Development Guidelines for Claude Code

These instructions are specifically for **Claude Code** when implementing this application.

### General

- **Always use the project structure** defined in Section 4. Do not deviate without reason.
- Use **Java 21** features where appropriate (records, sealed classes, pattern matching).
- Use **Spring Boot 3.x** conventions: auto-configuration, `application.yml` (not `.properties`).
- All database schema changes must be done via **Flyway migrations** in `src/main/resources/db/migration/`.
- Never use `spring.jpa.hibernate.ddl-auto=create` or `update` in non-test code.

### Backend

- Use **constructor injection** for all Spring beans (no `@Autowired` on fields).
- All service methods that modify data must be `@Transactional`.
- Use **Spring Data JPA** repositories; do not write boilerplate JDBC code.
- DTOs must be **Java records** for immutability.
- Use `ResponseEntity<?>` return types in controllers.
- Global exception handling via `@ControllerAdvice` with `@ExceptionHandler`.
- Write a `@SpringBootTest` integration test for each controller using `MockMvc`.
- Use `@DataJpaTest` for repository tests with an in-memory H2 database.

### Frontend

- Use **Angular 17+ standalone components** throughout — no NgModules.
- Use **Angular Signals** for local component state; use services with `signal()` for shared state.
- Use **Angular Reactive Forms** for all form handling.
- All HTTP calls go through a central `ApiService` that wraps `HttpClient`.
- Handle loading and error states explicitly in every component that makes API calls.
- Use `AsyncPipe` in templates to manage Observable subscriptions.
- Create **shared components** for reusable UI: `LoadingSpinnerComponent`, `ErrorMessageComponent`, `RecipeCardComponent`.

### Docker Compose

Provide a `docker-compose.yml` that starts:
- `postgres:15` with a named volume for data persistence
- The Spring Boot backend (with hot-reload via `spring-boot-devtools` or Docker Compose watch)
- (Optional) An Nginx reverse proxy for the Angular frontend in production

### Testing Strategy

| Layer | Tool | Coverage Target |
|---|---|---|
| Backend Unit | JUnit 5 + Mockito | Business logic in services |
| Backend Integration | MockMvc + @SpringBootTest | All controller endpoints |
| Repository | @DataJpaTest + H2 | Custom queries |
| Frontend Unit | Jest | Services, pipes |
| Frontend E2E | Playwright | Key user journeys (auth, calendar, brew mode) |

---

## 14. Environment Configuration

### `.env.example`
```env
# Database
POSTGRES_DB=brewmaster
POSTGRES_USER=brewmaster
POSTGRES_PASSWORD=changeme_in_production

# Backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/brewmaster
SPRING_DATASOURCE_USERNAME=brewmaster
SPRING_DATASOURCE_PASSWORD=changeme_in_production

# JWT
JWT_SECRET=your_256_bit_random_secret_here
JWT_EXPIRATION_MS=28800000

# Anthropic
ANTHROPIC_API_KEY=sk-ant-...

# CORS
FRONTEND_URL=http://localhost:4200
```

### `application.yml` (backend)
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-sonnet-4-20250514
  max-tokens: 4096

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS:28800000}

frontend:
  url: ${FRONTEND_URL:http://localhost:4200}

logging:
  level:
    com.brewmaster: DEBUG
    org.springframework.security: INFO
```

---

## 15. Future Features (Backlog)

The following features are planned for future versions and should be considered when making architectural decisions (e.g., keep the data model extensible):

- **Batch Notes & Brew Journal**: Detailed logging during and after brewing; photo uploads per session; gravity readings over time.
- **Water Chemistry Calculator**: Target water profile input; salt addition calculator; integration with recipe steps.
- **Inventory Management**: Track stock of ingredients; auto-deduct from inventory when a brew session is completed; reorder reminders.
- **Push Notifications**: Reminders for upcoming brew events; timer completion alerts via web push.
- **Community Recipes**: Public recipe sharing between users or groups.
- **Fermentation Tracker**: Log gravity readings over time; auto-calculate ABV; fermentation progress charts.
- **Admin Panel**: User management, system statistics.
- **Multi-Group Support**: Support multiple independent brewing groups within one instance.
- **Import/Export**: Import recipes from BeerXML format; export sessions to PDF.

---

*End of BrewMaster Application Specification v1.0.0*
