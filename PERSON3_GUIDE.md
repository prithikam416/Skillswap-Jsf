# Person 3 Guide – Database, Requests, Sessions & Reviews

## 1. What already existed (done by teammates) and what I added

| Area | Before | Now |
|---|---|---|
| Send request | `SearchBean.requestSkillExchange` → `ExchangeRequestDAO.createRequest` | unchanged, but duplicate check now covers PENDING **and** ACCEPTED, also in no-database mode |
| Accept / Reject | Buttons on `dashboard.xhtml`, no reason, no session details | New **`requests.xhtml`**: accept *with* date/time, mode, link/room, note; decline *with* reason |
| Sessions | Only a status flag (ACCEPTED) | Date/time, mode, location stored; either person can reschedule; Mark Completed |
| Reviews | Separate form on `reputation.xhtml` | Same form still works, plus inline review on each completed session and you can see your partner's review |
| Database | Tables had to be created by hand with `schema.sql`, MySQL had to be up before Tomcat started | `DBUtil.ensureSchema()` creates DB + tables + new columns automatically; retries every 15 s if MySQL starts late |
| Security | Rating DAO trusted the bean | `RatingDAO.addRating` itself checks: exchange COMPLETED, both are participants, not rated before |
| Bug fix | `dashboard.xhtml` crashed (HTTP 500) for any user who had a reverse match (`match.matchReason` does not exist) | Fixed → `match.compatibilityReason` |

### Files you own
```
src/main/java/com/skillswap/util/DBUtil.java            connection + auto schema
src/main/java/com/skillswap/dao/ExchangeRequestDAO.java requests, accept/reject, sessions, history
src/main/java/com/skillswap/dao/RatingDAO.java          reviews + reputation
src/main/java/com/skillswap/model/ExchangeRequest.java  entity (+ session fields)
src/main/java/com/skillswap/bean/ExchangeBean.java      JSF bean for requests page
src/main/java/com/skillswap/bean/RatingBean.java        JSF bean for reviews
src/main/webapp/requests.xhtml                          the new page
schema.sql / schema_person3.sql                         database scripts
```

## 2. IMPORTANT – JPA/Hibernate vs JDBC
Your work division says "JPA/Hibernate", but the project your friends built uses **plain JDBC** (`DriverManager`, `PreparedStatement`, DAO classes). There are no Hibernate jars, no `persistence.xml` and no `@Entity` classes.
**Do not claim JPA/Hibernate in the viva unless you convert it.** Say instead:
> "We connect Java to MySQL using JDBC with the DAO pattern. All queries are PreparedStatements, so they are safe from SQL injection. The DAO layer isolates the database from the JSF beans; JPA/Hibernate would be the next step to remove the hand-written SQL."
If your teacher insists on JPA/Hibernate, tell me and I'll convert the three tables you own (exchange_requests, ratings, user_skills links) – it needs Hibernate jars, which I could not download here.

## 3. How to run
1. Install MySQL, start it. Default login expected: **root / root** (change with env vars `SKILLSWAP_DB_USER`, `SKILLSWAP_DB_PASSWORD` if yours differs).
2. Eclipse → import project → Refresh (F5) → Run on Tomcat 9 → open `http://localhost:8080/SkillSwapJSF/index.xhtml`.
3. Nothing to import into MySQL – tables are created on first request. (You *may* still run `schema.sql`.)
4. Admin: `admin@skillswap.com` / `admin123`.

## 4. Demo script (2 browsers or 1 normal + 1 incognito)
1. Register **Alice** and **Bob**.
2. Bob → *My Skills* → add **Java** as *Teach*. Alice → *My Skills* → add **Java** as *Learn*.
3. Alice → *Search Mentors* → search "Java" → **Request Exchange** on Bob.
4. Bob → *Requests & Sessions* → incoming request → choose date, mode, room → **Accept & Book Session**. (Show **Decline** with a reason on a second request.)
5. Alice → *Requests & Sessions* → shows ACCEPTED + booking details. Try **Set / change schedule**.
6. Either → **Mark Completed**.
7. Both → *History & Reviews* → 5 stars + comment → **Submit Review**; reputation score changes; check *Leaderboard*.
8. Show MySQL: `SELECT id,sender_id,receiver_id,skill_name,status,session_date FROM exchange_requests;`

## 4b. Request life-cycle (draw this)
```
PENDING ──accept (receiver)──► ACCEPTED ──complete (either)──► COMPLETED ──► reviews
   │                              
   ├──decline (receiver)──► REJECTED
   └──withdraw (sender)───► CANCELLED
```

## 5. What to say (viva)
**Your part:** "I handled the database and the exchange workflow. MySQL stores users, skills, exchange requests and ratings. The DAO classes use JDBC to insert, update and read data. I implemented request management – a student sends a request, the mentor accepts or rejects it, the accepted request becomes a booked session with date, mode and place, either student marks it completed, and then both can review each other."

**Likely questions**
- *How do you stop someone accepting another student's request?* The SQL is `UPDATE … WHERE id=? AND receiver_id=? AND status='PENDING'` and the user id comes from the logged-in session, not from the page.
- *Duplicate requests?* The DAO checks for an open (pending/accepted) request for the same sender, receiver and skill.
- *Why can a review only be written once?* `UNIQUE(rater_id, exchange_id)` in MySQL, plus a check in `RatingDAO`.
- *What if MySQL is down?* `DBUtil.isDatabaseAvailable()` fails gracefully and the app uses in-memory lists; it retries MySQL every 15 seconds.
- *Relationships?* `exchange_requests.sender_id / receiver_id → users.id`; `ratings.exchange_id → exchange_requests.id`; all `ON DELETE CASCADE`.
- *Dates shown differ from MySQL Workbench by a few hours?* The JDBC URL uses `serverTimezone=UTC`; the app converts back to local time on display.

## 6. How it was tested
- 38 browser-level checks against a real MySQL-compatible server on an empty database (register, login, skills, search, request, duplicate block, accept with schedule, bad/past dates, reschedule, complete, reviews, reputation update, decline with reason, withdraw, access control, all pages load).
- 40 data-layer checks run twice (MySQL and no-database mode): permissions on every status change, duplicates, rating rules.
- Not verified: how the page *looks* (my sandbox blocks the Tailwind CDN; it uses the same classes as your other pages) and real e-mail sending (`EmailService` is not wired to any feature).

## 7. Known limitations (be honest if asked)
- No JPA/Hibernate (see §2). No e-mail/notification when a request arrives (future scope). Passwords use salted SHA-256 (fine for a college project; bcrypt would be better). The DB password `root/root` is a default.
