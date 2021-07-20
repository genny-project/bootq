package life.genny.entity;

public enum EEntityStatus {

    // This enum is designed for simplicity and speed.
    // The database will be expected to default to ACTIVE initially and we'll slowly bring in theother statuses

    ACTIVE,      // 0 - This status means that the entity should be picked up in all database fetches
    PENDING,     // 1 - This means that the entity is currently under construction and cannot be fetched under normal operation
    DELETED,     // 2 - This means that the entity is marked for deleted
    ARCHIVED,    // 3 - This means that the entity is not to be deleted , but is archived but not visible to normal operation
    DISABLED     // 4 - This means that the entity is currently not to be fetched, but is expected to possibly be returned to active status

}