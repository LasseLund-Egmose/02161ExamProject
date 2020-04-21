package dk.dtu.SoftEngExamProjectG18.Context;

import dk.dtu.SoftEngExamProjectG18.Core.Activity;
import dk.dtu.SoftEngExamProjectG18.Core.Employee;
import dk.dtu.SoftEngExamProjectG18.Core.Project;
import dk.dtu.SoftEngExamProjectG18.DB.CompanyDB;
import dk.dtu.SoftEngExamProjectG18.Exceptions.CommandException;
import dk.dtu.SoftEngExamProjectG18.Relations.EmployeeActivityIntermediate;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EmployeeInputContext extends InputContext {

    /*
        General
     */

    public static final Map<String, String[]> triggers = new HashMap<>() {{
        putAll(InputContext.getTriggersStatic());
        put("hours set", new String[]{"hours set {projectID} {activityID} {date} {hours}", "cmdSetHours"});
        put("hours submit", new String[]{"hours submit {projectID} {activityID} {date} {hours}", "cmdSubmitHours"});
        put("project activity markDone", new String[]{"project activity markDone {projectID} {activityID}", "cmdMarkActivityAsDone"});
        put("project create", new String[]{"project create {name} {billable}", "cmdCreateProject"});
        put("request assistance", new String[]{"request assistance {projectID} {activityID} {employeeID}", "cmdRequestAssistance"});
        put("request ooo", new String[]{"request ooo {type} {start} {end}", "cmdRequestOutOfOffice"});
        put("request overview daily", new String[]{"request overview daily", "cmdRequestDailyOverview"});
    }};

    public static Map<String, String[]> getTriggersStatic() {
        return EmployeeInputContext.triggers;
    }

    public String getSingularContextName() {
        return "an employee";
    }

    public Map<String, String[]> getTriggers() {
        return EmployeeInputContext.getTriggersStatic();
    }

    /*
        Command helpers
     */

    // String projectID, int activityID, Date date, int setHours
    @SuppressWarnings("unused")
    public void helperSetSubmitHours(String[] args, boolean shouldSet) throws CommandException, ParseException {
        assertArgumentsValid(args.length, 4);
        assertStringParseDateDoable(args[3]);
        assertStringParseIntDoable(args[4]);

        CompanyDB db = CompanyDB.getInstance();
        Project project = this.getProject(db, args[0]);
        Activity activity = this.getActivity(project, args[1]);

        Employee signedInEmployee = db.getSignedInEmployee();
        HashMap<String, EmployeeActivityIntermediate> trackedTime = activity.getTrackedTime();
        EmployeeActivityIntermediate employeeActivityIntermediate = trackedTime.get(signedInEmployee.getID());

        int minutes = Integer.parseInt(args[4]) * 60;
        Date date = this.formatter.parse(args[3]);

        if(shouldSet) {
            employeeActivityIntermediate.setMinutes(date, minutes);
            this.writeOutput("Hours set.");
        } else {
            employeeActivityIntermediate.addMinutes(date, minutes);
            this.writeOutput("Hours submitted.");
        }
    }

    /*
        Commands - warnings relating to use of reflection API are suppressed
     */

    // String name, boolean isBillable
    @SuppressWarnings("unused")
    public void cmdCreateProject(String[] args) throws CommandException {
        this.assertArgumentsValid(args.length, 0);
        this.assertValidProjectName(args[0]);

        Project project;
        if (args.length > 1) {
            boolean isBillable = Boolean.parseBoolean(args[1]);
            project = new Project(args[0], isBillable);
        } else {
            project = new Project(args[0]);
        }

        this.addProjectToDB(project);

        Activity activity = new Activity("First Activity", project);
        project.getActivities().put(activity.getID(), activity);
    }

    // String projectID, int activityID
    @SuppressWarnings("unused")
    public void cmdMarkActivityAsDone(String[] args) throws CommandException {
        this.assertArgumentsValid(args.length, 2);

        CompanyDB db = CompanyDB.getInstance();
        Project project = this.getProject(db, args[0]);
        Activity activity = this.getActivity(project, args[1]);

        activity.setDone(true);
        this.writeOutput("Activity completed.");
    }

    // String projectID, int activityID, String employeeID
    @SuppressWarnings("unused")
    public void cmdRequestAssistance(String[] args) throws CommandException {
        this.assertArgumentsValid(args.length, 3);

        CompanyDB db = CompanyDB.getInstance();
        Project project = this.getProject(db, args[0]);
        Activity activity = this.getActivity(project, args[1]);
        Employee employee = this.getEmployee(db, args[2]);

        Employee signedInEmployee = db.getSignedInEmployee();
        HashMap<String, HashMap<Integer, EmployeeActivityIntermediate>> signedInEmployeeActivities
                = signedInEmployee.getActivities();
        HashMap<Integer, EmployeeActivityIntermediate> signedEmployeeProjectActivities =
                signedInEmployeeActivities.get(project.getID());

        boolean signedInEmployeeIsNotAttachedToActivity = !signedEmployeeProjectActivities.containsKey(activity.getID());
        boolean otherEmployeeHasNoActivitySlotsLeft = employee.amountOfOpenActivities() == 0;

        if(signedInEmployeeIsNotAttachedToActivity) {
            String output = String.format("You are not allowing to work with the given activity, %s.", args[1]);
            throw new CommandException(output);
        }

        if(otherEmployeeHasNoActivitySlotsLeft) {
            String output = String.format(
                    "The employee, %s, you are requesting assistance from, has no room for any new activities at the moment.",
                    args[1]
            );
            throw new CommandException(output);
        }

        employee.getActivities().put(project.getID(), signedInEmployeeActivities.get(project.getID()));
        this.writeOutput("Assistance requested.");
    }

    @SuppressWarnings("unused")
    public void cmdRequestDailyOverview(String[] args) {
        // return true;
    }

    // OOOActivityType type, Date start, Date end
    @SuppressWarnings("unused")
    public void cmdRequestOutOfOffice(String[] args) throws CommandException {
        assertArgumentsValid(args.length, 3);

        CompanyDB db = CompanyDB.getInstance();
        Employee signedInEmployee = db.getSignedInEmployee();

        // TODO: Change below to exception-way of doing things
        /*
        OOOActivityType type = OOOActivityType.valueOf(args[0]);
        try {
            Date start = this.formatter.parse(args[1]);
            Date end = this.formatter.parse(args[2]);
            employee.getOOOactivities().add(new OutOfOfficeActivity(type, start, end));
            this.writeOutput("OOO activity added");
            return true;
        } catch (ParseException e) {
            this.writeOutput("Date must be in format " + this.formatter.toPattern());
            return false;
        }
        */
    }

    // String projectID, int activityID, Date date, int setHours
    @SuppressWarnings("unused")
    public void cmdSetHours(String[] args) throws CommandException, ParseException {
        this.helperSetSubmitHours(args, true);
    }

    // String projectID, int activityID, Date date, int addedHours
    @SuppressWarnings("unused")
    public void cmdSubmitHours(String[] args) throws CommandException, ParseException {
        this.helperSetSubmitHours(args, false);
    }

}
