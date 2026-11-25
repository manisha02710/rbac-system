package com.rbac.service;

/** Holds the single instance of each service (simple manual wiring, no framework). */
public final class Services {
    public final AuthorizationService authz = new JdbcAuthorizationService();
    public final AuditService audit = new AuditService();
    public final AuthService auth = new AuthService(authz, audit);
    public final UserService users = new UserService(authz, audit);
    public final RoleService roles = new RoleService(authz, audit);
    public final AcademicService academic = new AcademicService(authz, audit);
    public final CommunicationService comms = new CommunicationService(authz, audit);
    public final CourseService courses = new CourseService(authz, audit);
    public final MaterialService materials = new MaterialService(authz, audit, courses);
    public final AssignmentService assignments = new AssignmentService(authz, audit, courses);
    public final FeedbackService feedback = new FeedbackService(authz, audit);
}
