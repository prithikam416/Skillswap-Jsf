# SkillSwap - Campus Peer-to-Peer Skill Learning & Exchange Platform

SkillSwap is a JavaServer Faces (JSF) web application built for college campuses, enabling students to teach and learn skills from one another through peer-to-peer mentoring and reverse skill matching.

---

## 👥 Team Members

| USN | Name | Role | Responsibilities |
|---|---|---|---|
| **NNM24CS126** | **Prithika** | **Person 1** | **User Management, Profile, Skill Management, Skill Search & Reverse Match** |
| NNM24CS122 | Lassya | Person 2 | Admin Moderation, Session Tracking & Leaderboard |
| NNM24CS125 | Prianka | Person 3 | Exchange Requests, Accept/Reject Workflow & Reviews |

---

## 🚀 Person 1 Modules & Features Implemented

### 1. Module 1: User Management
- **Student Registration (`register.xhtml`)**: Full registration with USN, Name, Email, Password, Department, and Bio.
- **Login & Logout (`login.xhtml`)**: Session-scoped authentication handled via `UserBean.java`.
- **Student Profile (`profile.xhtml`)**: View and update profile, bio, department, and view dynamic mentor reputation score.
- **Skill Management (`skills.xhtml`)**:
  - Add & remove **Skills I Can Teach (Offered)** with proficiency levels (*Beginner*, *Intermediate*, *Expert*).
  - Add & remove **Skills I Want to Learn (Wanted)**.
  - Quick-add buttons for popular campus skills (*Java*, *Python*, *Web Dev*, *Figma*, *ML*).

### 2. Module 2: Skill Search & Reverse Match
- **Mentor Search (`search.xhtml`)**:
  - Search mentors by skill keyword (e.g., `Java`).
  - Ranked by skill compatibility percentage and mentor reputation score.
- **Reverse Match Engine (`reverse_match.xhtml`)**:
  - Automatically identifies peers who want to learn what you teach.
  - Calculates **100% Mutual Skill Swap** when both students can trade skills with each other.
- **Person 3 Integration Hook**:
  - **"Request Skill Exchange"** button passes the selected mentor and skill to the request module for Person 3's workflow.

---

## 🚀 Person 3 Modules & Features (Exchange Requests, Sessions & Reviews)

- **Requests & Sessions page (`requests.xhtml`)** – one place to manage the whole exchange life-cycle:
  - **Incoming requests** – Accept & book a session (date/time, Online/In-Person, link or room, note) or Decline with a reason.
  - **Requests I sent** – live status (PENDING / ACCEPTED / REJECTED / COMPLETED / CANCELLED), decline reason, booked session details, Withdraw button.
  - **Confirmed sessions** – shows when/where, change the schedule, **Mark Completed**.
  - **History & Reviews** – completed / declined / withdrawn exchanges; leave a 1-5 star review and read your partner's review.
- **Database layer** – `ExchangeRequestDAO`, `RatingDAO`, `DBUtil`:
  - Status changes are single guarded SQL `UPDATE`s (`... WHERE id=? AND receiver_id=? AND status='PENDING'`) so only the right person can do the right transition.
  - Tables and the new session columns are **created automatically** on first start (`DBUtil.ensureSchema`).
  - DB credentials can be changed with env vars `SKILLSWAP_DB_USER`, `SKILLSWAP_DB_PASSWORD`, `SKILLSWAP_DB_URL` (defaults: root/root on localhost).
  - Reviews are only allowed for COMPLETED exchanges, only between the two participants, once each; reputation score is recalculated after every review.
- See `PERSON3_GUIDE.md` for the full explanation, demo script and viva answers.

---

## 🛠 Tech Stack

- **Frontend**: JSF 2.3 (JavaServer Faces), XHTML, CSS3
- **Backend**: Java 21, Managed Beans (`@ManagedBean`, `@SessionScoped`)
- **Persistence**: MySQL via plain JDBC (DAO pattern, `PreparedStatement`s) with auto-detection & resilient in-memory fallback
- **Server**: Apache Tomcat v9.0

---

## 📦 How to Run in Eclipse

1. Clone or pull this repository into your Eclipse workspace.
2. In Eclipse, right-click the project $\rightarrow$ **Refresh** (`F5`).
3. Ensure **Apache Tomcat v9.0** is configured in Eclipse Servers.
4. (Optional) Run `schema.sql` in MySQL to initialize the database tables and seed data.
5. Right-click `index.xhtml` $\rightarrow$ **Run As** $\rightarrow$ **Run on Server** $\rightarrow$ Select **Apache Tomcat v9.0** $\rightarrow$ **Finish**.
6. Access the home page at: `http://localhost:8080/SkillSwapJSF/index.xhtml`.

## Abstract alignment update (branch `abstract-alignment`)
- **Six-table model**: `users`, `skills` (catalogue + category), `user_skills`, `exchange_requests`, `sessions`, `ratings`. Existing databases are migrated automatically on first start (session columns move into `sessions`, skill names are normalised into `skills`).
- **Reputation** = 60% average rating + 25% sessions completed + 15% consistency (see `ReputationCalculator`).
- **Category leaderboards**: "Top <Category> Mentors" on the Leaderboard page.

## JPA / Hibernate (branch `jpa-hibernate`)
- Hibernate 5.6.15 (jars in `WEB-INF/lib`) maps the six tables to entities in `com.skillswap.entity`:
  `UserEntity`, `SkillEntity`, `UserSkillEntity`, `ExchangeRequestEntity`, `SessionEntity`, `RatingEntity`
  (relationships: request -> sender/receiver users, request <-> session 1:1, rating -> rater/rated/request).
- `ExchangeRequestDAO` and `RatingDAO` (requests, accept/reject, sessions, reviews) now use JPA queries (JPQL) and
  transactions instead of hand-written SQL. Login/skills/admin DAOs still use JDBC against the same tables.
- `JpaUtil` builds the persistence unit in code from the same DB settings as `DBUtil` (no persistence.xml needed);
  `DBUtil` still creates/migrates the tables automatically. Without MySQL the in-memory fallback still works.

## Matches the project abstract (branch `abstract-complete`)
- **Six entities / tables:** `users`, `skills`, `user_skills`, `learning_requests`, `sessions`, `reviews` (older databases are renamed automatically).
- **Verified campus accounts:** registration only accepts college emails (default `@nmamit.in`; change with the
  `SKILLSWAP_COLLEGE_DOMAINS` environment variable or `-Dskillswap.college.domains=...`) and the student must enter a
  6-digit code sent by email before the first login (`verify.xhtml`). Without SMTP settings in `email.properties`
  the code is shown on the page (demo mode).
- **Three roles:** Student (Learner), Skill Provider (Mentor) - derived from the skills the student lists and shown on the dashboard - and Administrator.
