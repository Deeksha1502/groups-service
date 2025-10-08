# Play Framework & Akka to Pekko Migration - Visual Guide

## Current vs Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CURRENT STATE                            │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    Play Framework 2.7.2 (EOL)                     │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   Controllers Layer                        │  │
│  │  - BaseController                                          │  │
│  │  - CreateGroupController, UpdateGroupController, etc.     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              ↓ ↑                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │           Akka Actor System 2.5.22 (EOL)                   │  │
│  │  ┌──────────────────────────────────────────────────────┐  │  │
│  │  │  Actor Routing (smallest-mailbox-pool)              │  │  │
│  │  │  - HealthActor                                       │  │  │
│  │  │  - CreateGroupActor, UpdateGroupActor               │  │  │
│  │  │  - ReadGroupActor, SearchGroupActor                 │  │  │
│  │  │  - DeleteGroupActor                                 │  │  │
│  │  │  - UpdateGroupMembershipActor                       │  │  │
│  │  │  - CacheActor, GroupNotificationActor               │  │  │
│  │  └──────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌──────────────────────────────────────────────────────────────────┐
│                 External Dependencies                             │
│  - Cassandra (Database)                                           │
│  - Redis (Cache)                                                  │
│  - Content Service API                                            │
│  - User Org Service                                               │
└──────────────────────────────────────────────────────────────────┘

⚠️  ISSUES:
   - Akka BSL License (Commercial from 2.7+)
   - Play 2.7.2 EOL (No security patches)
   - Scala 2.12 (Old version)


┌─────────────────────────────────────────────────────────────────┐
│                         TARGET STATE                             │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    Play Framework 2.9.x (LTS)                     │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   Controllers Layer                        │  │
│  │  - BaseController (minimal changes)                       │  │
│  │  - CreateGroupController, UpdateGroupController, etc.     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              ↓ ↑                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │        Apache Pekko Actor System 1.0.x (Apache 2.0)        │  │
│  │  ┌──────────────────────────────────────────────────────┐  │  │
│  │  │  Actor Routing (smallest-mailbox-pool)              │  │  │
│  │  │  - HealthActor                                       │  │  │
│  │  │  - CreateGroupActor, UpdateGroupActor               │  │  │
│  │  │  - ReadGroupActor, SearchGroupActor                 │  │  │
│  │  │  - DeleteGroupActor                                 │  │  │
│  │  │  - UpdateGroupMembershipActor                       │  │  │
│  │  │  - CacheActor, GroupNotificationActor               │  │  │
│  │  └──────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌──────────────────────────────────────────────────────────────────┐
│                 External Dependencies                             │
│  - Cassandra (Database)  - No change                              │
│  - Redis (Cache)         - No change                              │
│  - Content Service API   - No change                              │
│  - User Org Service      - No change                              │
└──────────────────────────────────────────────────────────────────┘

✅  BENEFITS:
   - Apache 2.0 License (Free & Open Source)
   - Security patches and updates
   - Active community support
   - Future-proof architecture
```

## Migration Path - 3 Phases

```
CURRENT                 PHASE 1                PHASE 2               PHASE 3
───────                 ───────                ───────               ───────

Play 2.7.2      ───▶    Play 2.8.20    ───▶   Play 2.8.20   ───▶   Play 2.9.x
Akka 2.5.22     ───▶    Akka 2.6.20    ───▶   Pekko 1.0.x   ───▶   Pekko 1.0.x
Scala 2.12.11   ───▶    Scala 2.13.12  ───▶   Scala 2.13.12 ───▶   Scala 2.13.12
Java 11         ───▶    Java 11        ───▶   Java 11       ───▶   Java 11

Status: EOL           Status: Stable         Status: Stable       Status: LTS
Risk: High            Risk: Medium           Risk: Low            Risk: Low

Duration:             64-90 hours            60-90 hours          35-50 hours
Complexity:           Medium                 Medium               Low-Medium
Testing:              20-30 hours            20-30 hours          15-20 hours
```

## Code Changes Overview

### Phase 1: Play 2.8 + Akka 2.6
```java
// Minimal Java code changes
// Main changes in dependencies and configuration

// Configuration update needed:
akka.actor.allow-java-serialization = on
```

### Phase 2: Akka → Pekko (Major Changes)
```java
// BEFORE (Akka)
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedAbstractActor;

public class BaseActor extends UntypedAbstractActor {
    // Implementation
}

// AFTER (Pekko)
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.AbstractActor;

public class BaseActor extends AbstractActor {
    // Implementation
}
```

### Configuration Changes
```hocon
# BEFORE
akka {
  actor {
    provider = "akka.actor.LocalActorRefProvider"
  }
}

# AFTER
pekko {
  actor {
    provider = "org.apache.pekko.actor.LocalActorRefProvider"
  }
}
```

## Impact by Module

```
┌─────────────────────────────────────────────────────────────────┐
│                        Module Impact                             │
├──────────────────────┬──────────────┬────────────┬──────────────┤
│ Module               │ Files to     │ Complexity │ Testing      │
│                      │ Change       │            │ Effort       │
├──────────────────────┼──────────────┼────────────┼──────────────┤
│ sb-actor             │ 3 files      │ Medium     │ High         │
│ group-actors         │ 20+ files    │ Medium     │ High         │
│ service              │ 10+ files    │ Medium     │ High         │
│ platform-cache       │ 3 files      │ Low        │ Medium       │
│ sb-utils             │ 5 files      │ Low        │ Low          │
│ cassandra-utils      │ 0 files      │ None       │ Low          │
│ Configuration        │ 2 files      │ Low        │ Medium       │
│ Maven POMs           │ 9 files      │ Low        │ N/A          │
├──────────────────────┼──────────────┼────────────┼──────────────┤
│ TOTAL                │ ~70 files    │ Medium     │ High         │
└──────────────────────┴──────────────┴────────────┴──────────────┘
```

## Risk vs Benefit Analysis

```
High Risk/High Benefit  │  Low Risk/High Benefit
────────────────────────┼───────────────────────
                        │
    ⚠️ Stay on          │    ✅ Migrate to
     Akka 2.5           │      Pekko
     (EOL, License)     │      (Recommended)
                        │
────────────────────────┼───────────────────────
High Risk/Low Benefit   │  Low Risk/Low Benefit
                        │
    ❌ Buy Akka         │    ⚠️ Do nothing
     Commercial         │      (Not viable)
     License            │
                        │
```

## Timeline Visualization

```
Month 1              Month 2              Month 3              Month 4
───────              ───────              ───────              ───────
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   PHASE 1       │  │   PHASE 2       │  │   PHASE 3       │
│                 │  │                 │  │   (optional)    │
│ Play 2.8        │▶│ Akka→Pekko     │▶│ Play 2.9       │
│ Akka 2.6        │  │                 │  │                 │
│ Scala 2.13      │  │                 │  │                 │
│                 │  │                 │  │                 │
│ ████████░░░░░░  │  │ ████████░░░░░░  │  │ ██████░░░░░░░░  │
│ 64-90 hours     │  │ 60-90 hours     │  │ 35-50 hours     │
└─────────────────┘  └─────────────────┘  └─────────────────┘
       ↓                    ↓                    ↓
  ┌────────┐          ┌────────┐          ┌────────┐
  │ Test & │          │ Test & │          │ Test & │
  │ Deploy │          │ Deploy │          │ Deploy │
  └────────┘          └────────┘          └────────┘
```

## Success Metrics

```
┌──────────────────────────────────────────────────────────────────┐
│                        Success Criteria                           │
├──────────────────────────────────────────────────────────────────┤
│ ✅ All unit tests pass (100%)                                    │
│ ✅ All integration tests pass (100%)                             │
│ ✅ Performance degradation < 10%                                 │
│ ✅ No Akka dependencies remain                                   │
│ ✅ Zero production incidents in first month                      │
│ ✅ Code coverage maintained at 70%+                              │
│ ✅ Documentation updated                                         │
│ ✅ Team trained on Pekko                                         │
└──────────────────────────────────────────────────────────────────┘
```

## Decision Matrix

```
┌────────────────────┬──────────┬───────────┬──────────┬───────────┐
│ Option             │ Cost     │ Risk      │ Benefit  │ Recommend │
├────────────────────┼──────────┼───────────┼──────────┼───────────┤
│ Stay on Akka 2.5   │ Low      │ Very High │ None     │ ❌ NO     │
│ Upgrade Akka 2.7+  │ High     │ High      │ Low      │ ❌ NO     │
│ Migrate to Pekko   │ Medium   │ Medium    │ High     │ ✅ YES    │
│ Do Nothing         │ Zero     │ Very High │ Negative │ ❌ NO     │
└────────────────────┴──────────┴───────────┴──────────┴───────────┘
```

## Key Stakeholder Messages

### For Management
- **Risk:** Using current Akka version creates legal and security risks
- **Solution:** Migrate to Apache Pekko (open source, well-supported)
- **Cost:** 240-350 hours (~$30k-50k at industry rates)
- **ROI:** Avoid Akka license fees ($20k+/year), reduce security risk

### For Development Team
- **Good News:** Pekko is API-compatible with Akka
- **Challenge:** Need to update imports and configuration
- **Benefit:** More modern, supported framework
- **Learning:** Minimal - Pekko is nearly identical to Akka 2.6

### For QA Team
- **Impact:** Full regression testing required
- **Focus:** Actor behavior, performance, integration points
- **Timeline:** 55-80 hours testing across 3 phases
- **Tools:** Existing test suite, plus new Pekko TestKit

---

**For Full Details:** See [PLAY_AKKA_TO_PEKKO_MIGRATION_REPORT.md](./PLAY_AKKA_TO_PEKKO_MIGRATION_REPORT.md)
