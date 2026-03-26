# AI-DLC Adaptive Workflow - SmartHome Orchestrator

Welcome to the AI-Driven Development Life Cycle (AI-DLC) workflow for this project.

## How This Works

AI-DLC is an adaptive workflow system that guides development through three phases. Read the detail files in `.aidlc-rule-details/` for full instructions per stage.

## The Three Phases

### INCEPTION PHASE (Planning & Architecture)
- **Workspace Detection** (ALWAYS) → `.aidlc-rule-details/inception/workspace-detection.md`
- **Reverse Engineering** (CONDITIONAL - brownfield only) → `.aidlc-rule-details/inception/reverse-engineering.md`
- **Requirements Analysis** (ALWAYS) → `.aidlc-rule-details/inception/requirements-analysis.md`
- **User Stories** (CONDITIONAL) → `.aidlc-rule-details/inception/user-stories.md`
- **Application Design** (CONDITIONAL) → `.aidlc-rule-details/inception/application-design.md`
- **Units Generation** (CONDITIONAL) → `.aidlc-rule-details/inception/units-generation.md`
- **Workflow Planning** (ALWAYS) → `.aidlc-rule-details/inception/workflow-planning.md`

### CONSTRUCTION PHASE (Design, Implementation, Build & Test)
- **Functional Design** (CONDITIONAL) → `.aidlc-rule-details/construction/functional-design.md`
- **NFR Requirements** (CONDITIONAL) → `.aidlc-rule-details/construction/nfr-requirements.md`
- **NFR Design** (CONDITIONAL) → `.aidlc-rule-details/construction/nfr-design.md`
- **Infrastructure Design** (CONDITIONAL) → `.aidlc-rule-details/construction/infrastructure-design.md`
- **Code Generation** (ALWAYS) → `.aidlc-rule-details/construction/code-generation.md`
- **Build and Test** (ALWAYS) → `.aidlc-rule-details/construction/build-and-test.md`

### OPERATIONS PHASE
- **Operations** → `.aidlc-rule-details/operations/operations.md`

## Common Rules (apply in ALL phases)
- `.aidlc-rule-details/common/process-overview.md`
- `.aidlc-rule-details/common/depth-levels.md`
- `.aidlc-rule-details/common/error-handling.md`
- `.aidlc-rule-details/common/content-validation.md`
- `.aidlc-rule-details/common/overconfidence-prevention.md`
- `.aidlc-rule-details/common/question-format-guide.md`
- `.aidlc-rule-details/common/session-continuity.md`

## Key Principles
- Phases execute only when they add value
- INCEPTION focuses on "what" and "why"
- CONSTRUCTION focuses on "how" plus "build and test"
- Simple tasks may skip conditional stages; complex tasks get the full treatment
- Always wait for explicit user approval before proceeding to the next stage
- Developer stays in control — AI never makes autonomous architectural decisions

---

## Project-Specific Quality Standards (SmartHome Orchestrator)

These rules apply to ALL code generation and modifications in this project and are derived from the official requirements document.

### PMD (NFR-04)
- Always check generated Java code for PMD compliance — no critical or high violations allowed
- `ruleset.xml` in the project root defines the active PMD rules
- When in doubt, prefer PMD-safe patterns (specific exceptions, no empty blocks, no unused imports/variables, no `System.out.println` in production code)
- The CI build automatically fails on critical/high PMD findings

### Javadoc (NFR-06)
- EVERY `public` class, interface, and method in the Core Domain or API layer must have Javadoc
- Scope: all classes in `domain/`, `service/`, `repository/` (interfaces), `controller/`, `api/`
- Minimum Javadoc content:
  - A descriptive first sentence (purpose of the class/method)
  - `@param name description` for every parameter
  - `@return description` for non-void methods
  - `@throws ExceptionType description` if checked exceptions are thrown
- `private` methods and internal helper classes are exempt
