package in.copmap.domain;

public enum OperationStatus {
    PLANNED,     // created by planner, officers not yet on ground
    ACTIVE,      // officers deployed / operation in progress
    CLOSED,      // completed normally
    CANCELLED    // called off before completion
}
