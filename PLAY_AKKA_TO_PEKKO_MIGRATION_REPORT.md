# Play Framework Upgrade & Akka to Pekko Migration Compatibility Report

**Repository:** SNT01/groups-service  
**Report Date:** December 2024  
**Purpose:** Analysis and migration strategy for upgrading Play Framework and migrating from Akka to Apache Pekko

---

## Executive Summary

This report provides a comprehensive analysis of the current state of the groups-service repository and outlines a detailed migration strategy for:
1. Upgrading Play Framework from 2.7.2 to the latest version
2. Migrating from Akka to Apache Pekko (due to Akka's license change from open source to commercial BSL)

**Current State:**
- **Build Tool:** Maven (not SBT)
- **Play Framework:** 2.7.2 (released April 2019, EOL)
- **Akka Version:** 2.5.22 (released May 2019, EOL)
- **Scala Version:** 2.12.11
- **Java Version:** 11
- **Play2 Maven Plugin:** 1.0.0-rc5

**Migration Complexity:** Medium-High

---

## 1. Current Architecture Analysis

### 1.1 Technology Stack

| Component | Current Version | Status | Latest Version |
|-----------|----------------|--------|----------------|
| Play Framework | 2.7.2 | EOL | 3.0.5 / 2.9.5 |
| Akka | 2.5.22 | EOL | 2.9.x (BSL) |
| Scala | 2.12.11 | Active | 2.13.x / 3.x |
| Java | 11 | LTS | 21 LTS |
| Maven | 3.x | Active | 3.9.x |
| Play2 Maven Plugin | 1.0.0-rc5 | Outdated | 1.0.0-rc5 (no updates) |

### 1.2 Akka Usage Analysis

The codebase has **extensive Akka integration** with 52 Akka imports across the project:

#### Core Akka Components in Use:
1. **Actor System** (`ActorService.java`)
   - Custom actor system initialization
   - Actor registration and caching
   - Routing configuration with pool dispatchers

2. **Actor Implementations** (11 actors identified)
   - BaseActor (extends `UntypedAbstractActor`)
   - HealthActor
   - CreateGroupActor
   - ReadGroupActor
   - UpdateGroupActor
   - DeleteGroupActor
   - SearchGroupActor
   - UpdateGroupMembershipActor
   - CacheActor
   - GroupNotificationActor
   - DummyActor (test)

3. **Akka Routing**
   - Smallest-mailbox-pool routing strategy
   - Custom dispatchers (default-dispatcher, health-dispatcher, group-dispatcher)
   - Router configuration in application.conf

4. **Akka Patterns**
   - Pattern matching with `Patterns.ask()`
   - Future conversions with Scala compatibility layer
   - `ActorRef` for message passing
   - TestKit for unit testing

5. **Key Dependencies:**
   ```xml
   - akka-actor_2.12:2.5.22
   - akka-testkit_2.12:2.5.22
   - play-akka-http-server_2.12:2.7.2
   ```

### 1.3 Play Framework Usage

#### Play Components:
1. **Controllers** (BaseController pattern)
   - Play MVC controllers
   - CompletionStage-based async actions
   - Integration with Akka actors

2. **Configuration**
   - application.conf with Akka configuration
   - Routes file with Play routing DSL
   - Custom action creators and error handlers

3. **Play Dependencies:**
   ```xml
   - play_2.12:2.7.2
   - play-guice_2.12:2.7.2
   - play-netty-server_2.12:2.7.2
   - play-akka-http-server_2.12:2.7.2
   - play-logback_2.12:2.7.2
   - filters-helpers_2.12:2.7.2
   - play-test_2.12:2.7.2
   ```

4. **Build Integration**
   - Maven-based build (NOT SBT)
   - play2-maven-plugin for packaging
   - `mvn play2:run` for development
   - `mvn play2:dist` for distribution

### 1.4 Module Structure

```
groups-service/
├── sb-actor/              # Core actor framework
├── group-actors/          # Business logic actors
├── service/               # Play Framework web service
├── sb-utils/              # Utilities
├── sb-telemetry-utils/    # Telemetry
├── platform-cache/        # Redis cache (3 Scala files)
├── cassandra-utils/       # Database utilities
└── reports/               # Reporting module
```

---

## 2. Why Migrate?

### 2.1 Akka License Change

**Critical Issue:** In September 2022, Lightbend changed Akka's license from Apache 2.0 to Business Source License (BSL) 1.1:
- Akka versions 2.7+ require commercial licensing for production use
- Free only for:
  - Non-production use
  - Companies with < $25M annual revenue
  - Open source projects

**Impact:** Using Akka 2.7+ in production without a license violates terms and creates legal/compliance risk.

### 2.2 Apache Pekko Solution

**Apache Pekko** is an Apache Software Foundation fork of Akka 2.6.x:
- Maintained under Apache 2.0 license (free and open source)
- Drop-in replacement for Akka
- Active development and community support
- Compatible API with minimal code changes
- Current version: Pekko 1.0.x (based on Akka 2.6.x)

### 2.3 Play Framework End of Life

**Play 2.7.x Status:**
- Released: April 2019
- End of Life: May 2021
- Security vulnerabilities not patched
- Missing features from modern versions
- Compatibility issues with newer libraries

**Benefits of Upgrading:**
- Security patches and bug fixes
- Performance improvements
- Better Java 17/21 support
- Modern async patterns
- Active community support

---

## 3. Migration Strategy

### 3.1 Overall Approach

**Recommended Strategy:** Phased migration in the following order:

1. **Phase 1:** Upgrade to Play 2.8.x + Akka 2.6.x (intermediate stable state)
2. **Phase 2:** Migrate from Akka 2.6.x to Pekko 1.0.x
3. **Phase 3:** Upgrade to Play 2.9.x or 3.0.x (if needed)

**Rationale:**
- Play 2.8.x is compatible with Akka 2.6.x
- Pekko 1.0.x is based on Akka 2.6.x (easier migration)
- Gradual changes reduce risk
- Each phase can be tested independently

### 3.2 Phase 1: Upgrade to Play 2.8.x + Akka 2.6.x

#### 3.2.1 Version Changes

```xml
<!-- From -->
<play2.version>2.7.2</play2.version>
<typesafe.akka.version>2.5.22</typesafe.akka.version>
<scala.version>2.12.11</scala.version>

<!-- To -->
<play2.version>2.8.20</play2.version>  <!-- Latest 2.8.x -->
<typesafe.akka.version>2.6.20</typesafe.akka.version>  <!-- Latest 2.6.x -->
<scala.version>2.13.12</scala.version>  <!-- Recommended for Play 2.8 -->
```

#### 3.2.2 Breaking Changes in Play 2.8

1. **Scala 2.13 Migration Required**
   - Play 2.8 requires Scala 2.13 (current: 2.12)
   - Affects all `_2.12` artifacts → `_2.13`
   - Binary incompatibility requires recompilation

2. **Java API Changes**
   - `Http.Context` removed, use `Http.Request` (mostly done)
   - Some method signatures changed
   - Deprecated APIs removed

3. **Guice Changes**
   - Updated to Guice 5.x
   - Some injection patterns may need updates

4. **Configuration Changes**
   - Some configuration keys renamed
   - HOCON parsing stricter

#### 3.2.3 Breaking Changes in Akka 2.6

1. **Actor API Changes**
   - `UntypedAbstractActor` still supported (good for this project)
   - Typed actors recommended but not required
   - Serialization changes

2. **Configuration Changes**
   - Java serialization disabled by default
   - Need explicit serialization configuration:
   ```hocon
   akka.actor.allow-java-serialization = on
   ```

3. **Akka HTTP**
   - Akka HTTP decoupled from Akka streams
   - Version alignment required

4. **Cluster/Remoting**
   - Artery is default remoting (not used in this project)
   - Classic remoting deprecated

#### 3.2.4 Maven Plugin Challenges

**Critical Issue:** play2-maven-plugin is not actively maintained:
- Last release: 1.0.0-rc5 (2019)
- Limited Play 2.8+ support
- No official Play 3.0 support

**Solutions:**
1. **Continue with Maven + Play 2.8.x**
   - Possible but may require workarounds
   - Manual dependency management
   - Limited tooling support

2. **Migrate to SBT** (Recommended)
   - Official build tool for Play
   - Better Play integration
   - Active maintenance
   - Better ecosystem support

#### 3.2.5 Estimated Effort for Phase 1

| Task | Effort | Complexity |
|------|--------|------------|
| Update Maven dependencies | 4 hours | Low |
| Migrate Scala 2.12 → 2.13 | 8 hours | Medium |
| Fix compilation errors | 8-16 hours | Medium |
| Update Akka configuration | 4 hours | Low |
| Test and fix issues | 16-24 hours | Medium-High |
| Update documentation | 4 hours | Low |
| **Total** | **44-60 hours** | **Medium** |

### 3.3 Phase 2: Migrate Akka 2.6.x to Pekko 1.0.x

#### 3.3.1 Version Changes

```xml
<!-- Remove Akka dependencies -->
<dependency>
  <groupId>com.typesafe.akka</groupId>
  <artifactId>akka-actor_2.13</artifactId>
  <version>2.6.20</version>
</dependency>

<!-- Add Pekko dependencies -->
<dependency>
  <groupId>org.apache.pekko</groupId>
  <artifactId>pekko-actor_2.13</artifactId>
  <version>1.0.2</version>
</dependency>
```

#### 3.3.2 Code Changes Required

1. **Package Rename (Automated)**
   - All `akka.*` imports → `org.apache.pekko.*`
   - All `com.typesafe.akka` → `org.apache.pekko`
   - Can be done with search-and-replace or IDE refactoring

2. **Configuration Changes**
   - `akka { }` blocks → `pekko { }`
   - Configuration file references updated

3. **Class Names**
   - Most classes identical (e.g., `ActorSystem`, `ActorRef`)
   - `UntypedAbstractActor` → `AbstractActor` (minor change)

#### 3.3.3 Pekko Compatibility Matrix

| Play Version | Pekko Version | Compatible |
|--------------|---------------|------------|
| Play 2.8.x | Pekko 1.0.x | ⚠️ Manual integration |
| Play 2.9.x | Pekko 1.0.x | ✅ Official support |
| Play 3.0.x | Pekko 1.0.x | ✅ Official support |

**Note:** Play 2.8.x predates Pekko, so you need to manually manage Pekko dependencies and configuration.

#### 3.3.4 Migration Steps

1. **Update Dependencies**
   ```xml
   <!-- Core actor -->
   org.apache.pekko:pekko-actor_2.13:1.0.2
   
   <!-- Testing -->
   org.apache.pekko:pekko-testkit_2.13:1.0.2
   
   <!-- HTTP (if using Play Pekko HTTP) -->
   org.apache.pekko:pekko-http_2.13:1.0.2
   org.apache.pekko:pekko-stream_2.13:1.0.2
   ```

2. **Find and Replace**
   ```bash
   # In all .java files
   import akka.actor.* → import org.apache.pekko.actor.*
   import akka.event.* → import org.apache.pekko.event.*
   import akka.pattern.* → import org.apache.pekko.pattern.*
   import akka.util.* → import org.apache.pekko.util.*
   import akka.testkit.* → import org.apache.pekko.testkit.*
   import akka.routing.* → import org.apache.pekko.routing.*
   ```

3. **Update Configuration**
   ```hocon
   # application.conf
   akka { } → pekko { }
   thisActorSystem.akka { } → thisActorSystem.pekko { }
   ```

4. **Update Play Integration**
   - Remove `play-akka-http-server` dependency
   - May need to use alternative HTTP server or wait for Play 2.9

#### 3.3.5 Files Requiring Changes

**Java Files (52+ imports):**
- `/sb-actor/src/main/java/org/sunbird/actor/core/ActorService.java`
- `/sb-actor/src/main/java/org/sunbird/actor/core/ActorCache.java`
- `/group-actors/src/main/java/org/sunbird/Application.java`
- `/group-actors/src/main/java/org/sunbird/actors/*.java` (11 actors)
- `/service/app/controllers/BaseController.java`
- `/service/test/controllers/DummyActor.java`
- All test files using `akka.testkit`

**Configuration Files:**
- `/service/conf/application.conf` (200+ lines of Akka config)
- `/group-actors/src/main/resources/application.conf`

**Scala Files:**
- `/platform-cache/src/main/scala/org/sunbird/cache/*.scala` (3 files)

#### 3.3.6 Estimated Effort for Phase 2

| Task | Effort | Complexity |
|------|--------|------------|
| Update Maven dependencies | 2 hours | Low |
| Automated find-replace (imports) | 2 hours | Low |
| Update configuration files | 4 hours | Low |
| Fix compilation errors | 4-8 hours | Low-Medium |
| Update Play integration | 8-16 hours | Medium-High |
| Comprehensive testing | 16-24 hours | Medium |
| Update documentation | 4 hours | Low |
| **Total** | **40-60 hours** | **Medium** |

### 3.4 Phase 3: Upgrade to Play 2.9.x or 3.0.x (Optional)

#### 3.4.1 Play 2.9.x (Recommended)

**Advantages:**
- Official Pekko support
- Maintains Java 11 compatibility
- Evolutionary upgrade from 2.8.x
- Stable and production-ready

**Changes:**
```xml
<play2.version>2.9.2</play2.version>
<pekko.version>1.0.2</pekko.version>
```

**Breaking Changes:**
- Full Pekko integration (no more Akka)
- Some API refinements
- Dependency updates

**Estimated Effort:** 20-30 hours

#### 3.4.2 Play 3.0.x (Future Option)

**Advantages:**
- Latest features
- Long-term support
- Best performance

**Challenges:**
- Requires Java 17+
- Significant breaking changes
- New routing DSL
- Major API changes

**Recommendation:** Wait until project is stable on Play 2.9 + Pekko before considering 3.0.x

**Estimated Effort:** 60-80 hours

---

## 4. Potential Issues and Challenges

### 4.1 Technical Challenges

| Issue | Impact | Mitigation |
|-------|--------|------------|
| **Maven Plugin Limitations** | High | Consider migrating to SBT or manual packaging |
| **Scala Version Migration** | High | Thorough testing, recompile all dependencies |
| **Binary Compatibility** | Medium | Update all Scala libraries to 2.13 versions |
| **Play-Akka Integration** | Medium | Use intermediate Play 2.8 + Akka 2.6 state |
| **Testing Complexity** | Medium | Comprehensive test suite, staged rollout |
| **Third-party Dependencies** | Low-Medium | Check compatibility, update as needed |

### 4.2 Specific Code Concerns

#### 4.2.1 UntypedAbstractActor Usage

**Current:** All actors extend `UntypedAbstractActor`
- ✅ Supported in Akka 2.6 and Pekko 1.0
- ⚠️ Deprecated in favor of typed actors
- **Action:** No immediate change required, but consider typed actors long-term

#### 4.2.2 Scala Compatibility Layer

**Current:** Uses `scala.compat.java8.FutureConverters`
- ⚠️ Deprecated in Scala 2.13
- **Action:** Replace with `scala.jdk.FutureConverters` or Java 8+ CompletableFuture

#### 4.2.3 Custom Actor System

**Current:** `ActorService.java` with custom initialization
- ✅ Pattern compatible with Pekko
- **Action:** Test thoroughly, verify routing configuration

#### 4.2.4 Java Serialization

**Current:** Uses Java serialization for actors
```hocon
serializers {
  java = "akka.serialization.JavaSerializer"
}
```
- ⚠️ Security risk, disabled by default in Akka 2.6+
- **Action:** Either enable explicitly or migrate to better serialization (Jackson, Protobuf)

### 4.3 Build and Deployment Challenges

#### 4.3.1 Maven Plugin Obsolescence

**Issue:** play2-maven-plugin is unmaintained
**Options:**
1. Continue with manual dependency management
2. Migrate to SBT (significant effort but better long-term)
3. Use alternative packaging methods

#### 4.3.2 Docker Build Changes

**Current Dockerfile:**
```dockerfile
CMD java -cp '/home/sunbird/group-service-1.0.0/lib/*' play.core.server.ProdServerStart
```

**Potential Changes:**
- Classpath adjustments for new dependencies
- Configuration file updates
- Environment variable changes

### 4.4 Testing Challenges

#### 4.4.1 Test Coverage

**Current:**
- Unit tests with JUnit
- Actor tests with Akka TestKit
- Play integration tests

**Actions:**
- Update TestKit to Pekko TestKit
- Verify all tests pass after migration
- Add regression tests for actor behavior

#### 4.4.2 Integration Testing

**Dependencies:**
- Content service APIs
- User org service
- Cassandra
- Redis

**Actions:**
- Verify all integrations work post-migration
- Update mock services if needed

---

## 5. Alternative Approaches

### 5.1 Option A: Stay on Akka (Not Recommended)

**Approach:** Remain on Akka 2.5.x or upgrade to 2.6.x only

**Pros:**
- No migration needed
- Minimal code changes
- Lower short-term effort

**Cons:**
- ❌ Akka 2.5.x is EOL (no security patches)
- ❌ Akka 2.6.x will eventually be EOL
- ❌ Cannot upgrade to Akka 2.7+ without commercial license
- ❌ Legal/compliance risk
- ❌ Community support diminishing

**Recommendation:** ⛔ Not viable long-term

### 5.2 Option B: Direct Migration to Play 3.0 + Pekko

**Approach:** Skip Play 2.8/2.9 and go directly to Play 3.0

**Pros:**
- Latest features
- Single major upgrade
- Future-proof

**Cons:**
- ❌ Requires Java 17+ (current: Java 11)
- ❌ Massive breaking changes
- ❌ Higher risk
- ❌ More extensive testing needed

**Recommendation:** ⚠️ Only if you can afford 100+ hours and need Play 3.0 features

### 5.3 Option C: Phased Migration (Recommended)

**Approach:** Play 2.7 → 2.8 → Pekko → 2.9 (as outlined in Section 3)

**Pros:**
- ✅ Lower risk per phase
- ✅ Can test and validate each step
- ✅ Gradual learning curve
- ✅ Rollback easier if issues arise
- ✅ Akka 2.6 is more stable than 2.5

**Cons:**
- More phases = more time overall
- Need intermediate releases

**Recommendation:** ✅ Best balance of risk and effort

### 5.4 Option D: Migrate to SBT First

**Approach:** Convert Maven → SBT, then upgrade frameworks

**Pros:**
- ✅ Better Play integration
- ✅ Official build tool
- ✅ Easier future upgrades
- ✅ Better IDE support

**Cons:**
- ❌ Additional learning curve
- ❌ CI/CD pipeline changes
- ❌ Team training needed
- ❌ Build scripts rewrite

**Effort:** 40-60 hours for SBT migration

**Recommendation:** ⚠️ Consider as Phase 0 if planning multiple Play upgrades

---

## 6. Dependency Compatibility Matrix

### 6.1 Current Dependencies

| Dependency | Current | Compatible With Play 2.8 | Compatible With Pekko 1.0 |
|------------|---------|---------------------------|---------------------------|
| Jackson | 2.13.5 | ✅ (update to 2.14+) | ✅ |
| Logback | 1.2.3 | ✅ (update to 1.4+) | ✅ |
| Commons Lang3 | 3.9 | ✅ | ✅ |
| Commons Collections4 | 4.3 | ✅ | ✅ |
| Netty | 4.1.44 | ✅ (update to 4.1.94+) | ✅ |
| JUnit | 4.13.2 | ✅ | ✅ |
| Cassandra Driver | 3.x | ✅ | ✅ |
| Redis/Jedis | 2.6.2 | ⚠️ (very old, update to 4.x) | ✅ |

### 6.2 New Dependencies Needed

**For Pekko Migration:**
```xml
<!-- Replace Akka dependencies with Pekko -->
<dependency>
  <groupId>org.apache.pekko</groupId>
  <artifactId>pekko-actor_2.13</artifactId>
  <version>1.0.2</version>
</dependency>
<dependency>
  <groupId>org.apache.pekko</groupId>
  <artifactId>pekko-testkit_2.13</artifactId>
  <version>1.0.2</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.apache.pekko</groupId>
  <artifactId>pekko-slf4j_2.13</artifactId>
  <version>1.0.2</version>
</dependency>
```

**For Play 2.9:**
```xml
<dependency>
  <groupId>com.typesafe.play</groupId>
  <artifactId>play-pekko-http-server_2.13</artifactId>
  <version>2.9.2</version>
</dependency>
```

---

## 7. Cost-Benefit Analysis

### 7.1 Costs

| Phase | Development Effort | Testing Effort | Total Effort | Risk Level |
|-------|-------------------|----------------|--------------|------------|
| Phase 1: Play 2.8 + Akka 2.6 | 44-60 hours | 20-30 hours | 64-90 hours | Medium |
| Phase 2: Akka → Pekko | 40-60 hours | 20-30 hours | 60-90 hours | Medium |
| Phase 3: Play 2.9 (optional) | 20-30 hours | 15-20 hours | 35-50 hours | Low-Medium |
| **Total (Full Migration)** | **104-150 hours** | **55-80 hours** | **159-230 hours** | **Medium** |

**Additional Costs:**
- Code review: 20-30 hours
- Documentation updates: 10-15 hours
- Training/knowledge transfer: 10-20 hours
- Contingency (20%): 40-55 hours

**Total Project Estimate:** 240-350 hours (6-9 weeks with 1 developer)

### 7.2 Benefits

#### Immediate Benefits:
1. **Legal Compliance** ✅
   - Avoid Akka BSL licensing issues
   - Open source license (Apache 2.0)

2. **Security** 🔒
   - Security patches for Play
   - Security patches for Pekko
   - Updated dependencies

3. **Community Support** 👥
   - Active Pekko community
   - Apache Foundation backing
   - Modern Play ecosystem

#### Long-term Benefits:
1. **Maintainability** 🔧
   - Modern codebase
   - Better documentation
   - Easier to hire developers

2. **Performance** ⚡
   - Optimizations in newer versions
   - Better async handling
   - Improved throughput

3. **Future-Proofing** 🚀
   - Clear upgrade path to Play 3.0
   - Pekko long-term roadmap
   - Java 17/21 ready

4. **Cost Savings** 💰
   - No Akka commercial license fees
   - Reduced technical debt
   - Easier future upgrades

### 7.3 Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Breaking changes cause downtime | Medium | High | Phased rollout, extensive testing |
| Maven plugin incompatibility | High | Medium | Manual dependency management or SBT migration |
| Performance regression | Low | Medium | Load testing, benchmarking |
| Integration issues | Medium | Medium | Mock services, integration tests |
| Developer learning curve | Medium | Low | Training, documentation |
| Timeline overruns | Medium | Medium | Buffer time, agile approach |

**Overall Risk Level:** Medium (manageable with proper planning)

---

## 8. Recommendations

### 8.1 Immediate Actions (Priority 1)

1. **✅ Accept This Report**
   - Review findings with team
   - Get stakeholder approval
   - Allocate resources

2. **🔒 Security Assessment**
   - Audit current Akka usage for license compliance
   - Document any existing legal risk
   - Set deadline for migration

3. **📝 Create Migration Plan**
   - Break down into sprints
   - Assign team members
   - Set milestone dates

4. **🧪 Set Up Test Environment**
   - Clone production-like environment
   - Prepare test data
   - Set up monitoring

### 8.2 Migration Approach (Priority 2)

**Recommended Path:**

```
Current State                    Target State
-------------                    ------------
Play 2.7.2                      Play 2.9.x
Akka 2.5.22        ------>      Pekko 1.0.x
Scala 2.12.11                   Scala 2.13.x
Java 11                         Java 11 (upgrade to 17 later)
Maven                           Maven (consider SBT)
```

**Milestones:**
1. **Month 1:** Phase 1 - Upgrade to Play 2.8 + Akka 2.6 + Scala 2.13
2. **Month 2:** Phase 2 - Migrate Akka 2.6 to Pekko 1.0
3. **Month 3:** Phase 3 - Upgrade to Play 2.9 (if needed)

### 8.3 Alternative Recommendations

**If Timeline is Critical:**
- Skip Phase 3 initially
- Stay on Play 2.8 + Pekko 1.0 (works but requires manual integration)
- Plan Play 2.9 upgrade for later

**If Resources are Limited:**
- Consider hiring external consultants for migration
- Use automated tools for package renaming
- Focus on critical paths first

**If Risk Tolerance is Low:**
- Start with non-production environment
- Implement feature flags for rollback
- Run parallel systems temporarily

### 8.4 Build Tool Decision

**Question:** Should we migrate to SBT?

**Short Answer:** Not required, but beneficial if planning multiple Play upgrades.

**Decision Matrix:**

| Factor | Stay with Maven | Migrate to SBT |
|--------|----------------|----------------|
| Effort | 0 hours | 40-60 hours |
| Play Support | Limited | Excellent |
| Team Familiarity | High | Low-Medium |
| Future Upgrades | Harder | Easier |
| Community Support | Lower | Higher |

**Recommendation:** ⚠️ Defer SBT migration to Phase 4 (after Pekko migration) unless experiencing major Maven issues.

---

## 9. Migration Checklist

### 9.1 Pre-Migration Tasks

- [ ] Review and approve this report
- [ ] Get stakeholder buy-in
- [ ] Allocate development resources (240-350 hours)
- [ ] Set up test environment
- [ ] Back up current codebase
- [ ] Document current architecture
- [ ] Audit all Akka usage locations
- [ ] Identify all dependencies
- [ ] Set up monitoring/metrics
- [ ] Create rollback plan

### 9.2 Phase 1: Play 2.8 + Akka 2.6

- [ ] Update Maven parent POM versions
- [ ] Update all Play dependencies to 2.8.x
- [ ] Update Akka dependencies to 2.6.x
- [ ] Migrate Scala 2.12 to 2.13 dependencies
- [ ] Recompile all modules
- [ ] Fix compilation errors
- [ ] Update Akka configuration for 2.6
- [ ] Update Java serialization settings
- [ ] Run all unit tests
- [ ] Run integration tests
- [ ] Performance testing
- [ ] Security scanning
- [ ] Code review
- [ ] Deploy to staging
- [ ] User acceptance testing
- [ ] Deploy to production
- [ ] Monitor for issues

### 9.3 Phase 2: Akka to Pekko

- [ ] Update Maven dependencies (Akka → Pekko)
- [ ] Run automated find-replace for imports
- [ ] Update configuration files (akka → pekko)
- [ ] Update custom actor system initialization
- [ ] Update routing configuration
- [ ] Fix compilation errors
- [ ] Update all test cases
- [ ] Verify actor behavior unchanged
- [ ] Run all unit tests
- [ ] Run integration tests
- [ ] Performance testing and comparison
- [ ] Load testing
- [ ] Security scanning
- [ ] Code review
- [ ] Deploy to staging
- [ ] User acceptance testing
- [ ] Deploy to production
- [ ] Monitor for issues
- [ ] Update documentation

### 9.4 Phase 3: Play 2.9 (Optional)

- [ ] Update Play dependencies to 2.9.x
- [ ] Add Pekko integration dependencies
- [ ] Update configuration
- [ ] Fix compilation errors
- [ ] Run all tests
- [ ] Performance testing
- [ ] Deploy and monitor

### 9.5 Post-Migration Tasks

- [ ] Update developer documentation
- [ ] Update deployment documentation
- [ ] Update CI/CD pipelines
- [ ] Archive old dependencies
- [ ] Remove unused code
- [ ] Optimize performance
- [ ] Knowledge transfer to team
- [ ] Retrospective meeting
- [ ] Plan next steps

---

## 10. Code Examples

### 10.1 Package Import Changes

**Before (Akka):**
```java
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import akka.routing.FromConfig;
import akka.event.Logging;
import akka.event.DiagnosticLoggingAdapter;
import akka.testkit.javadsl.TestKit;
```

**After (Pekko):**
```java
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.AbstractActor;  // Note: UntypedAbstractActor → AbstractActor
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.apache.pekko.routing.FromConfig;
import org.apache.pekko.event.Logging;
import org.apache.pekko.event.DiagnosticLoggingAdapter;
import org.apache.pekko.testkit.javadsl.TestKit;
```

### 10.2 Configuration Changes

**Before (Akka):**
```hocon
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  loglevel = "INFO"
  actor {
    provider = "akka.actor.LocalActorRefProvider"
    serializers {
      java = "akka.serialization.JavaSerializer"
    }
  }
}
```

**After (Pekko):**
```hocon
pekko {
  loggers = ["org.apache.pekko.event.slf4j.Slf4jLogger"]
  loglevel = "INFO"
  actor {
    provider = "org.apache.pekko.actor.LocalActorRefProvider"
    serializers {
      java = "org.apache.pekko.serialization.JavaSerializer"
    }
  }
}
```

### 10.3 Actor Class Changes

**Before (Akka):**
```java
import akka.actor.UntypedAbstractActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.event.Logging;

public abstract class BaseActor extends UntypedAbstractActor {
  final DiagnosticLoggingAdapter logger = Logging.getLogger(this);
  
  @Override
  public void onReceive(Object message) throws Throwable {
    // Implementation
  }
}
```

**After (Pekko):**
```java
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.event.DiagnosticLoggingAdapter;
import org.apache.pekko.event.Logging;

public abstract class BaseActor extends AbstractActor {
  final DiagnosticLoggingAdapter logger = Logging.getLogger(this);
  
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(Object.class, message -> {
        // Implementation
      })
      .build();
  }
}
```

**Note:** If you want to keep `onReceive()` method, you can still extend `AbstractActor` and override `createReceive()` to delegate to `onReceive()`.

### 10.4 Maven POM Changes

**Before:**
```xml
<properties>
  <play2.version>2.7.2</play2.version>
  <typesafe.akka.version>2.5.22</typesafe.akka.version>
  <scala.major.version>2.12</scala.major.version>
  <scala.version>2.12.11</scala.version>
</properties>

<dependency>
  <groupId>com.typesafe.akka</groupId>
  <artifactId>akka-actor_${scala.major.version}</artifactId>
  <version>${typesafe.akka.version}</version>
</dependency>
```

**After (Phase 2 - Pekko):**
```xml
<properties>
  <play2.version>2.8.20</play2.version>
  <pekko.version>1.0.2</pekko.version>
  <scala.major.version>2.13</scala.major.version>
  <scala.version>2.13.12</scala.version>
</properties>

<dependency>
  <groupId>org.apache.pekko</groupId>
  <artifactId>pekko-actor_${scala.major.version}</artifactId>
  <version>${pekko.version}</version>
</dependency>
```

---

## 11. Testing Strategy

### 11.1 Unit Testing

**Focus Areas:**
- Actor message handling
- Business logic
- Utility functions

**Tools:**
- JUnit 4 (current) or upgrade to JUnit 5
- Pekko TestKit
- Mockito/PowerMock

**Test Coverage Target:** Maintain 70%+ coverage

### 11.2 Integration Testing

**Focus Areas:**
- Actor system initialization
- Message routing
- Play controller integration
- Database connections (Cassandra, Redis)

**Approach:**
- Use embedded Cassandra for tests
- Use embedded Redis for tests
- Mock external service calls

### 11.3 Performance Testing

**Metrics to Monitor:**
- Request throughput (requests/second)
- Response latency (p50, p95, p99)
- Actor mailbox sizes
- Memory usage
- CPU usage

**Tools:**
- JMeter or Gatling
- Application Performance Monitoring (APM)

**Acceptance Criteria:**
- No more than 10% performance degradation
- No memory leaks
- Stable under load

### 11.4 Regression Testing

**Critical Paths:**
- Health check endpoints
- Group CRUD operations
- Search functionality
- Membership updates
- Notification sending

**Test Data:**
- Use production-like data volumes
- Test edge cases
- Test error scenarios

---

## 12. Support and Resources

### 12.1 Official Documentation

**Apache Pekko:**
- Website: https://pekko.apache.org
- GitHub: https://github.com/apache/incubator-pekko
- Migration Guide: https://pekko.apache.org/docs/pekko/current/project/migration-guides.html

**Play Framework:**
- Website: https://www.playframework.com
- Migration Guide 2.7 → 2.8: https://www.playframework.com/documentation/2.8.x/Migration28
- Migration Guide 2.8 → 2.9: https://www.playframework.com/documentation/2.9.x/Migration29

**Scala:**
- Scala 2.13 Migration: https://docs.scala-lang.org/overviews/core/collections-migration-213.html

### 12.2 Community Resources

- Stack Overflow tags: `apache-pekko`, `playframework`
- Gitter/Discord: Pekko community chat
- Mailing lists: Apache Pekko dev/users lists

### 12.3 Tools

**Migration Tools:**
- IntelliJ IDEA: Refactoring tools
- VS Code: Find and replace with regex
- Maven: dependency:tree, dependency:analyze

**Testing Tools:**
- JUnit 5
- Pekko TestKit
- ScalaTest (for Scala code)
- Gatling (performance testing)

---

## 13. Conclusion

### 13.1 Summary

This repository requires migration from Akka to Pekko due to Akka's license change. The migration is **technically feasible** with **medium complexity** and **estimated 240-350 hours** of effort.

**Key Findings:**
1. ✅ Codebase is well-structured for migration
2. ✅ Pekko provides API compatibility with Akka 2.6.x
3. ⚠️ Play 2.7.2 is EOL and needs upgrade
4. ⚠️ Maven plugin limitations may require workarounds
5. ✅ No critical blockers identified

### 13.2 Go/No-Go Decision

**Recommendation: ✅ GO for Migration**

**Reasons:**
1. **Legal Necessity:** Akka BSL license creates compliance risk
2. **Technical Debt:** Play 2.7.2 is EOL (no security patches)
3. **Feasibility:** Migration is technically achievable
4. **Cost-Benefit:** Benefits outweigh costs long-term
5. **Community:** Apache Pekko has strong backing

**Recommended Timeline:**
- **Q1 2025:** Phase 1 (Play 2.8 + Akka 2.6)
- **Q2 2025:** Phase 2 (Akka → Pekko)
- **Q3 2025:** Phase 3 (Play 2.9) - Optional

### 13.3 Success Criteria

Migration is successful when:
- ✅ All tests pass
- ✅ Performance is equal or better
- ✅ No Akka dependencies remain
- ✅ Production deployment is stable
- ✅ Documentation is updated
- ✅ Team is trained

### 13.4 Next Steps

1. **Review this report** with technical team and stakeholders
2. **Get approval** for migration project
3. **Allocate resources** (2-3 developers for 2-3 months)
4. **Create detailed sprint plan** based on this report
5. **Set up test environment** for migration work
6. **Begin Phase 1** with Play 2.8 + Akka 2.6 upgrade

---

## 14. Appendices

### Appendix A: Glossary

- **Akka:** Actor-based toolkit for JVM (now BSL licensed)
- **Pekko:** Apache fork of Akka 2.6.x (Apache 2.0 licensed)
- **BSL:** Business Source License (requires commercial license for production use with revenue > $25M)
- **Play Framework:** Web framework for Java/Scala
- **Actor Model:** Concurrency model using message-passing actors
- **EOL:** End of Life (no longer supported)
- **SBT:** Scala Build Tool (official build tool for Scala/Play projects)

### Appendix B: File Change Summary

**Total Files to Modify:** ~70 files

| File Type | Count | Change Type |
|-----------|-------|-------------|
| Java source files | 52+ | Import statements |
| Configuration files | 2 | Package names, syntax |
| POM files | 9 | Dependencies |
| Scala files | 3 | Import statements |
| Test files | 15+ | Import statements, TestKit |

### Appendix C: Dependency Version Matrix

| Library | Current | Play 2.8 | Play 2.9 | Notes |
|---------|---------|----------|----------|-------|
| Play | 2.7.2 | 2.8.20 | 2.9.2 | LTS versions |
| Akka | 2.5.22 | 2.6.20 | N/A | Migrate to Pekko |
| Pekko | N/A | 1.0.2 | 1.0.2 | Apache 2.0 |
| Scala | 2.12.11 | 2.13.12 | 2.13.12 | Major version bump |
| Java | 11 | 11 | 11/17 | Java 17 for Play 3.0 |

### Appendix D: Contact Information

For questions or clarifications about this report, contact:
- Project repository: https://github.com/SNT01/groups-service
- Apache Pekko: https://pekko.apache.org/community/
- Play Framework: https://www.playframework.com/community

---

**Report Version:** 1.0  
**Last Updated:** December 2024  
**Report Status:** Final - Ready for Review  
**Classification:** Internal Use

