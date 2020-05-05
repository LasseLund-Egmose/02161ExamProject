package dk.dtu.SoftEngExamProjectG18.Business;

import dk.dtu.SoftEngExamProjectG18.General.DateFormatter;
import dk.dtu.SoftEngExamProjectG18.General.Exceptions.AccessDeniedException;
import dk.dtu.SoftEngExamProjectG18.Business.Interfaces.Extractable;

import java.util.*;

public class EmployeeActivityIntermediate implements Extractable<EmployeeActivityIntermediate> {

    public static EmployeeActivityIntermediate initAssociation(Employee employee, Activity activity) throws AccessDeniedException {
        employee.assertOpenActivities();
        return new EmployeeActivityIntermediate(employee, activity);
    }

    public static EmployeeActivityIntermediate getAssociation(Employee employee, Activity activity) throws AccessDeniedException {
        HashMap<String, EmployeeActivityIntermediate> trackedTime = activity.getTrackedTime();
        EmployeeActivityIntermediate employeeActivityIntermediate = trackedTime.get(employee.getID());

        if(employeeActivityIntermediate == null) {
            throw new AccessDeniedException("You are not associated with one or more of these projects.");
        }

        return employeeActivityIntermediate;
    }

    // Date -> Minutes
    protected HashMap<String, Integer> minutesSpent = new HashMap<>();

    protected Activity activity;
    protected Employee employee;

    public EmployeeActivityIntermediate(Employee e, Activity a) {
        this.employee = e;
        this.activity = a;

        Project project = a.getProject();

        HashMap<String, HashMap<Integer, EmployeeActivityIntermediate>> employeeProjectActivities = e.getActivities();
        if(!employeeProjectActivities.containsKey(project.getID())) {
            employeeProjectActivities.put(project.getID(), new HashMap<>());
        }

        employeeProjectActivities.get(project.getID()).put(a.getID(), this);

        a.getTrackedTime().put(e.getID(), this);
    }

    public Activity getActivity() {
        return this.activity;
    }

    public Employee getEmployee() {
        return this.employee;
    }

    public int getMinutes(Date d) throws IllegalArgumentException {
        return this.minutesSpent.getOrDefault(DateFormatter.formatDate(d), 0);
    }

    public HashMap<String, Integer> getMinutesSpent() {
        return this.minutesSpent;
    }

    public int getTotalMinutes() {
        return this.minutesSpent.values().stream().mapToInt(i -> i).sum();
    }

    public void setMinutes(Date d, int minutes) throws IllegalArgumentException {
        if(minutes < 0) {
            String output = String.format("The set number of work minutes has to be more than or equal to 0. %s received.", minutes);
            throw new IllegalArgumentException(output);
        }

        this.minutesSpent.put(DateFormatter.formatDate(d), minutes);
    }

    public void submitMinutes(Date d, int minutes) throws IllegalArgumentException {
        if(minutes < 0) {
            String output = String.format("The submitted number of work minutes has to be more than or equal to 0. %s received.", minutes);
            throw new IllegalArgumentException(output);
        }

        this.minutesSpent.put(DateFormatter.formatDate(d), this.getMinutes(d) + minutes);
    }

    /*
        Table extraction methods
     */

    @Override
    public ArrayList<HashMap<String, String>> extract(String context, HashMap<String, Object> metaData, ArrayList<? extends Extractable<?>> collection) {
        return context.equals("overview") ? this.extractOverview(collection) : null;
    }

    public ArrayList<HashMap<String, String>> extractOverview(ArrayList<? extends Extractable<?>> collection) {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();

        for(Extractable<?> extractable : collection) {
            if(!(extractable instanceof EmployeeActivityIntermediate)) { continue; }

            EmployeeActivityIntermediate eai = (EmployeeActivityIntermediate) extractable;

            for(String date : eai.minutesSpent.keySet()) {
                int minutes = eai.minutesSpent.get(date);

                HashMap<String, String> entry = new HashMap<>();
                entry.put("Employee", eai.getEmployee().getName());
                entry.put("Date", date);
                entry.put("Minutes", String.valueOf(minutes));

                result.add(entry);
            }
        }

        return result;
    }
}
