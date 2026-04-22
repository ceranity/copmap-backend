package in.copmap.domain;

/**
 * Three-tier police hierarchy mapped to the workflow in the spec.
 *
 *  - OFFICER     : beat/field officer — receives assignments, sends location pings,
 *                  raises/acknowledges alerts on the ground.
 *  - PLANNER     : station officer (SHO-level) — creates patrols and bandobast,
 *                  assigns officers, monitors live map, closes operations.
 *  - SUPERVISOR  : DCP / ACP-level — read-only oversight across station boundaries,
 *                  can audit and escalate.
 */
public enum Role {
    OFFICER,
    PLANNER,
    SUPERVISOR
}
