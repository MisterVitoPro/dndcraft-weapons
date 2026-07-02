# Phase 3 Implementation Status - Low-Priority QA Fixes

**Date:** 2026-07-01  
**Status:** In Progress  
**Plan:** Implement 31 low-priority optimization and documentation fixes

---

## Execution Strategy

Phase 3 consists of 31 low-priority fixes across 6 categories:

### Wave 1: Logging & Observability (13 fixes)
- [x] **LOG-001** Missing vanilla-mapped role tag binding logs (SpecRegistry)
- [x] **LOG-002** Missing role cache lifecycle instrumentation (invalidation, rebuild timing)
- [x] **LOG-003** Silent tooltip injection callback registration (WeaponTooltipInjector)
- [ ] **LOG-005** AcquisitionCatalog.validate() diagnostic gaps
- [ ] **LOG-006** Incomplete villager trade logging
- [ ] **LOG-007** WitherTrophyHandler missing item count metrics
- [ ] **LOG-008** WeaponTradeRegistrar incomplete breakdown logging
- [ ] **LOG-009** Missing vanilla-mapped spec count in init summary
- [ ] **LOG-010** No cache performance metrics available
- [ ] **LOG-011** Validation error message lacks diagnostic context
- [ ] **LOG-012** Wiki generation lacks per-page error details
- [ ] **LOG-013** No error context if acquisition validation fails

### Wave 2: Performance Optimizations (7 fixes)
- [ ] **PERF-001** String allocation overhead in wiki rendering
- [ ] **PERF-002** Synchronized cache build contention (partially fixed in P2-008, optimize further)
- [ ] **PERF-003** Property.values() reflection in tooltip formatting
- [ ] **PERF-004** Unbounded maps with no cleanup
- [ ] **PERF-005** O(N) registry lookups at mod init validation
- [ ] **PERF-006** Repeated string manipulation in friendly formatters
- [ ] **PERF-007** No parallelization in wiki generation

### Wave 3: Build Configuration & Compatibility (4 fixes)
- [ ] **CONFIG-002** Kotlin 2.3.21 compatibility with JDK 17 target
- [ ] **CONFIG-004** Missing gradle.lock validation (partial fix in P2-003, complete)
- [ ] **CONFIG-008** No per-version language files (assumption documentation)
- [ ] **CONFIG-009** Loader version variation unvalidated (compatibility matrix)

### Wave 4: Code Cleanup (3 fixes)
- [ ] **CORR-003** Unused language key (tooltip.dndweapons.property.versatile.with_dice)
- [ ] **ARCH-004** Unnecessary translations for vanilla-mapped weapons

### Wave 5: Security Edge Cases (2 fixes)
- [ ] **SEC-004** Missing sanitization in wiki string transformations
- [ ] **SEC-005** Unsafe translation key prefix validation (partially fixed in P1-003, harden)

### Wave 6: Supply Chain & Other (2 fixes)
- [ ] **SUP-001** Kotlin version mismatch in gradle plugin vs FLK
- [ ] **SUP-002** No security scanning in supply chain

---

## Test Coverage

TDD approach: Tests written first, implementation makes them pass.

### Test Framework
- **Full suite:** `./gradlew test`
- **Single-file:** `./gradlew test --tests {file}`

### Baseline Failing Tests
Starting with clean baseline (all existing tests pass).

---

## Progress Tracking

### Implementation Tracking

| Fix ID | Category | Status | Tests Written | Tests Passing | Implementation | Notes |
|--------|----------|--------|---------------|---------------|-----------------|-------|
| LOG-001 | Logging | In Progress | ✓ | Pending | Pending | Vanilla-mapped logging in SpecRegistry.bindRoleTag() |
| LOG-002 | Logging | In Progress | ✓ | Pending | Pending | Cache lifecycle timing metrics |
| LOG-003 | Logging | In Progress | ✓ | Pending | Pending | Tooltip injection callback registration |
| PERF-003 | Performance | Planned | - | - | - | Cache Property.values() reflection |
| PERF-005 | Performance | Planned | - | - | - | Add registry index for O(1) lookups |

---

## Artifacts Generated

- **Cycle Directory:** `docs/plan-runner/2026-07-01/cycle-2/`
- **Manifest:** `docs/plan-runner/2026-07-01/cycle-2/manifest.json`
- **Bug Reports:** `docs/plan-runner/2026-07-01/cycle-2/bugs/`
- **Wave Plan:** `docs/plan-runner/2026-07-01/cycle-2/wave-plan.json`

---

## Success Criteria

- [ ] All 31 fixes implemented or documented as deferred
- [ ] Test coverage for each fix (TDD approach)
- [ ] All tests passing
- [ ] No regressions in P1/P2 fixes
- [ ] Atomic commits per wave
- [ ] Final PR with all Phase 3 work

---

## Notes

This is Phase 3 of multi-phase QA remediation:
- **Phase 1:** 8 critical fixes (COMPLETED)
- **Phase 2:** 10 high-priority fixes (COMPLETED)
- **Phase 3:** 31 low-priority fixes (IN PROGRESS)

Phase 3 focuses on optimization, documentation, and code cleanliness rather than correctness.
