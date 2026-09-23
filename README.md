# Server Pay Tracker (Finance Tracker)

A full-stack web app for tracking pay as a server — hourly wages, cash/card
tips, multiple jobs, and user-defined custom attributes — with statistical
profitability analysis and shift forecasting. Started as a terminal-only Java
learning project and evolved into a secured, multi-user, deployed web
application with a companion Python data-analysis notebook.

**Live demo:** [finance-tracker-j622.onrender.com](https://finance-tracker-j622.onrender.com)
Log in with `test` / `test` to explore a pre-seeded account with over a
year of realistic synthetic shift history across two jobs.

> Note: the free-tier host spins down after inactivity, so the first load
> may take 30–60 seconds to wake up. The demo account is shared and
> editable by anyone who visits — don't rely on its data staying intact.

## Features

- **Multi-job support** — track shifts across multiple employers, switch
  between them from a dropdown, with all stats scoped per job.
- **Shift tracking** — date, shift type (Morning/Afternoon/Evening/Night),
  hours, wage, cash and card tips, and a free-text role field with
  autocomplete suggestions.
- **Custom attributes** — add arbitrary user-defined fields to any shift
  (e.g. hairstyle, shoes) via a flexible JSONB column, with an
  auto-generated table view and a dedicated **attribute profitability**
  breakdown ranking each attribute's values by average earnings.
- **Summary & profitability views** — quick date-range buttons (1/3/6/12
  months) or a custom range, most/least profitable day+type+role
  combinations, and an adjustable minimum-sample-size threshold so small,
  unreliable groupings can be filtered out.
- **Shift forecasting** — enter an upcoming 1–2 week schedule and get
  data-driven "keep vs. swap" recommendations, based on a blend of
  same-season-last-year and recent-weeks averages.
- **Accounts & security** — registration, session-based login, and
  per-user data isolation via Spring Security and BCrypt password hashing.
- **Statistical analysis (Python)** — a companion Jupyter notebook
  (`analysis/shift_analysis.ipynb`) that goes beyond the app's own
  averages: t-tests, OLS regression (with a documented multicollinearity
  diagnosis and fix), a per-job breakdown, and season-over-season
  visualizations validating the forecasting logic's core assumption.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.4.1, Spring Security (session auth,
  BCrypt), plain JDBC (no ORM)
- **Database:** PostgreSQL, hosted on Supabase
- **Frontend:** HTML, CSS, vanilla JavaScript (served as Spring Boot
  static resources)
- **Build tool:** Maven (via the included Maven Wrapper — no local Maven
  install required)
- **Deployment:** Docker, hosted on Render (auto-deploys on push to `main`)
- **Analysis:** Python (pandas, statsmodels, scipy, matplotlib, SQLAlchemy)
  in a Jupyter notebook, querying the same live Postgres database

## Requirements

- **JDK 21** (LTS) installed locally — [Oracle JDK downloads](https://www.oracle.com/java/technologies/downloads/#java21)
  or [Adoptium Temurin](https://adoptium.net/)
- The shared **database password**, provided separately by the project
  owner (not stored in this repo)
- *(Optional, for the analysis notebook)* Python 3.10+ and the packages in
  `analysis/requirements.txt`

You do **not** need Maven installed — the Maven Wrapper handles that
automatically.

## Getting Started (running the app locally)

1. **Clone the repository**
   ```
   git clone <repo-url>
   cd BruBurgerFinanceTracker
   ```

2. **Set the database password as an environment variable**

   Request `SUPABASE_DB_PASSWORD` from the project owner, then set it:

   - **Windows (PowerShell), current session only:**
     ```
     $env:SUPABASE_DB_PASSWORD="the-password"
     ```
   - **Windows, permanent:** System Properties → Environment Variables →
     User variables → New (`SUPABASE_DB_PASSWORD`).
   - **Mac/Linux:** add `export SUPABASE_DB_PASSWORD="the-password"` to
     `~/.zshrc` or `~/.bashrc`, then `source` it.

3. **Build and run**
   ```
   ./mvnw spring-boot:run     # Mac/Linux
   mvnw.cmd spring-boot:run   # Windows
   ```

4. **Open it in your browser**
   ```
   http://localhost:8080
   ```
   Register a new account, or log in as `test` / `test` to see the
   pre-seeded demo data.

## Running the analysis notebook

```
cd analysis
python -m venv .venv
.venv\Scripts\Activate.ps1   # Windows; use source .venv/bin/activate on Mac/Linux
pip install -r requirements.txt
```
Open `shift_analysis.ipynb` in Jupyter or VS Code, set `SUPABASE_DB_PASSWORD`
the same way as above, and run all cells. It connects directly to the same
Postgres database the Java app uses.

## Notes

- Shift data is scoped per-account — you'll only ever see your own shifts,
  jobs, and forecasts.
- The database password is intentionally excluded from version control
  (see `.gitignore`). Never commit it directly into source files.
- Both the Render (app) and Supabase (database) free tiers pause after a
  period of inactivity — expect a slow first load if the project hasn't
  been visited recently.

## Project History

This app started as a terminal-only Java program using flat-file storage,
and was incrementally rebuilt through several real architecture changes:
SQLite → PostgreSQL migration, terminal CLI → Spring Boot REST API, adding
a web frontend and full authentication, multi-job and custom-attribute
support, a shift-forecasting feature, deployment to Render, and a
statistical analysis layer in Python. Commit history reflects each stage
of that evolution.
