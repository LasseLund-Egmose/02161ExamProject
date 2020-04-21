package dk.dtu.SoftEngExamProjectG18.tests;

import dk.dtu.SoftEngExamProjectG18.Context.InputContext;
import dk.dtu.SoftEngExamProjectG18.DB.CompanyDB;
import dk.dtu.SoftEngExamProjectG18.Exceptions.CommandException;
import dk.dtu.SoftEngExamProjectG18.tests.Util.CmdResponse;
import dk.dtu.SoftEngExamProjectG18.tests.Util.TestHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract public class BaseSteps {

    protected final CompanyDB db = CompanyDB.getInstance();

    protected void callCmd(InputContext context, String method, String[] args) {
        CommandException cmdException = null;
        String response = null;

        try {
            Method m = context.getClass().getMethod(method, String[].class);
            m.invoke(context, (Object) args);
        } catch (InvocationTargetException ite) {
            if(ite.getTargetException() instanceof CommandException) {
                cmdException = (CommandException) ite.getTargetException();
            } else {
                response = "An internal error occurred.";
            }
        } catch (Exception e) {
            response = "An internal error occurred.";
        }

        if(response == null) {
            response = context.getOutput();
        }

        context.resetOutput();

        TestHolder.getInstance().response = new CmdResponse(response, cmdException);
    }

    protected void callCmdClean(InputContext context, String method, String[] args) throws Exception {
        this.callCmd(context, method, args);
        CmdResponse response = TestHolder.getInstance().response;
        if(!response.isClean()) {
            throw new Exception("Received command response is not clean!");
        }
    }

}