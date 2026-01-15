# Kitchensink Modernization - Step-by-Step Implementation Guide

## Migration Approach: Contract-Driven Development

This document outlines a professional migration approach that you can explain to interviewers.

---

## The Methodology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              CONTRACT-DRIVEN MIGRATION APPROACH                             │
│                                                                             │
│  "I don't just rewrite code - I capture the behavior first,                │
│   then build to match that behavior."                                       │
└─────────────────────────────────────────────────────────────────────────────┘

Step 1: Document Legacy Behavior (the "Contract")
         ↓
Step 2: Write Tests Against Legacy (verify contract is correct)
         ↓
Step 3: Build New App to Pass Same Tests (preserve behavior)
         ↓
Step 4: Enhance with Production Features (improve beyond legacy)
```

---

## PHASE 1: Understand & Document (30 minutes)

### Step 1.1: Run Legacy App

```bash
cd /Users/nitina/IdeaProjects/jboss-eap-quickstarts
./wildfly-31.0.1.Final/bin/standalone.sh
```

### Step 1.2: Explore Legacy API

```bash
# List all members
curl http://localhost:8080/kitchensink/rest/members

# Get single member
curl http://localhost:8080/kitchensink/rest/members/0

# Create a member
curl -X POST http://localhost:8080/kitchensink/rest/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","phoneNumber":"1234567890"}'
```

### Step 1.3: Document API Contract

Create `docs/LEGACY_API_CONTRACT.md` with:
- All endpoints (URL, method)
- Request/response formats
- Validation rules
- Error responses

**Interview Talking Point:**
> "I started by thoroughly documenting the legacy API contract. This serves as the 
> specification for the migration - if the new app matches this contract, we know 
> the migration preserves behavior."

---

## PHASE 2: Write Contract Tests (30 minutes)

### Step 2.1: Create Contract Test Class

File: `src/test/java/.../contract/MemberApiContractTest.java`

Key tests:
- `GET /members` - returns list
- `GET /members/{id}` - returns single member
- `GET /members/{id}` - returns 404 for non-existing
- `POST /members` - creates member successfully
- `POST /members` - rejects duplicate email (409)
- `POST /members` - rejects invalid name (400)
- `POST /members` - rejects invalid email (400)
- `POST /members` - rejects invalid phone (400)

### Step 2.2: Run Tests Against Legacy

```bash
# Make sure legacy app is running on port 8080
# Set BASE_URL in test to legacy: "http://localhost:8080/kitchensink/rest"
mvn test -Dtest=MemberApiContractTest
```

✅ All tests should PASS against legacy

**Interview Talking Point:**
> "Before writing any new code, I wrote API contract tests against the legacy app.
> These tests capture the expected behavior and become my migration specification.
> If these tests pass on the new app, I know the migration is functionally correct."

---

## PHASE 3: Build New App (2-3 hours)

### Step 3.1: Create Domain Model

File: `src/main/java/.../model/Member.java`

```java
@Document(collection = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    private String id;
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 25)
    @Pattern(regexp = "[^0-9]*", message = "Name must not contain numbers")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Indexed(unique = true)
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 12)
    @Pattern(regexp = "\\d+", message = "Phone must contain only digits")
    private String phoneNumber;
}
```

**Why MongoDB @Document instead of JPA @Entity?**
- Schema flexibility (no ALTER TABLE needed)
- Natural document model fits the entity
- Horizontal scalability for future growth

### Step 3.2: Create Repository

File: `src/main/java/.../repository/MemberRepository.java`

```java
public interface MemberRepository extends MongoRepository<Member, String> {
    Optional<Member> findByEmail(String email);
    List<Member> findAllByOrderByNameAsc();
}
```

**Interview Talking Point:**
> "Spring Data MongoDB provides the same repository abstraction as JPA. 
> The migration was smooth because I kept the same interface pattern."

### Step 3.3: Create Service Layer

File: `src/main/java/.../service/MemberService.java`

```java
public interface MemberService {
    List<Member> findAll();
    Member findById(String id);
    Member register(Member member);
}
```

File: `src/main/java/.../service/MemberServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    
    @Override
    public List<Member> findAll() {
        return memberRepository.findAllByOrderByNameAsc();
    }
    
    @Override
    public Member findById(String id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
    }
    
    @Override
    public Member register(Member member) {
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            throw new DuplicateEmailException(member.getEmail());
        }
        return memberRepository.save(member);
    }
}
```

### Step 3.4: Create Exception Handling

File: `src/main/java/.../exception/MemberNotFoundException.java`
File: `src/main/java/.../exception/DuplicateEmailException.java`
File: `src/main/java/.../exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }
    
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("email", "Email already taken"));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
```

### Step 3.5: Create REST Controller

File: `src/main/java/.../controller/MemberController.java`

```java
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    
    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.findAll();
    }
    
    @GetMapping("/{id}")
    public Member getMemberById(@PathVariable String id) {
        return memberService.findById(id);
    }
    
    @PostMapping
    public ResponseEntity<Void> createMember(@Valid @RequestBody Member member) {
        memberService.register(member);
        return ResponseEntity.ok().build();
    }
}
```

### Step 3.6: Run Contract Tests Against New App

```bash
# Start new app on port 8080
# Change BASE_URL in test to: "http://localhost:8080/api"
mvn test -Dtest=MemberApiContractTest
```

✅ All tests should PASS against new app!

**Interview Talking Point:**
> "After implementing the new app, I ran the same contract tests against it.
> All tests passed, proving the migration preserves functional behavior.
> This gives confidence for the cutover."

---

## PHASE 4: Add Production Features (1-2 hours)

### Step 4.1: Add JWT Authentication

- Create `JwtTokenProvider` - generate/validate tokens
- Create `JwtAuthenticationFilter` - extract token from requests
- Update `SecurityConfig` - protect endpoints

### Step 4.2: Add RBAC (Role-Based Access Control)

- Create `User`, `Role`, `Permission` entities
- Create `UserService`, `RoleService`
- Add `@PreAuthorize` to controller methods

### Step 4.3: Add Audit Logging

- Create `AuditLog` entity
- Create `AuditService` with `@Async`
- Create `AuditAspect` for automatic logging

### Step 4.4: Add Other Production Features

- Rate limiting
- Caching
- Structured logging with correlation IDs
- Health checks
- API documentation (Swagger)

---

## Implementation Order (Checklist)

```
PHASE 1: UNDERSTAND
□ Run legacy app
□ Test all endpoints manually
□ Document API contract

PHASE 2: CONTRACT TESTS
□ Write tests for GET /members
□ Write tests for GET /members/{id}
□ Write tests for POST /members (success)
□ Write tests for POST /members (validation errors)
□ Run tests against legacy → ALL PASS

PHASE 3: BUILD NEW APP
□ Create Member model (@Document)
□ Create MemberRepository
□ Create MemberService interface
□ Create MemberServiceImpl
□ Create custom exceptions
□ Create GlobalExceptionHandler
□ Create MemberController
□ Run contract tests → ALL PASS

PHASE 4: ENHANCE
□ Add SecurityConfig (permit all initially)
□ Add JWT authentication
□ Add RBAC
□ Add audit logging
□ Add rate limiting
□ Add caching
□ Add Swagger documentation
□ Add unit tests
□ Add integration tests with Testcontainers
```

---

## Interview Talking Points

### On Approach
> "I used a contract-driven migration approach. Instead of blindly rewriting code,
> I first documented the API contract, wrote tests to capture expected behavior,
> then built the new app to pass those same tests. This ensures functional equivalence."

### On Risk Mitigation
> "The contract tests serve as a safety net. If any test fails after a change,
> I know immediately that I've broken compatibility. This reduces migration risk."

### On Why This Approach for Larger Codebases
> "For a larger codebase, I'd apply this same pattern at the service level.
> Each microservice would have its contract tests, and we'd migrate one at a time
> using the strangler fig pattern. The key is having automated verification
> that behavior is preserved."

### On Testing Strategy
> "I have three levels of tests:
> 1. Contract tests - verify API compatibility with legacy
> 2. Unit tests - verify individual components work correctly
> 3. Integration tests - verify components work together with real MongoDB"

### On MongoDB Choice
> "MongoDB was chosen because the Member entity is a natural document.
> The schema flexibility means we can evolve the model without migrations.
> Spring Data MongoDB made the transition smooth - same repository pattern as JPA."

