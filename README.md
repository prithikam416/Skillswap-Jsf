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
