package quickutils.transactionManager;

 
/**
 *
 * @author JIBOYE, Oluwagbemiro Olaoluwa <gbenroscience@yahoo.com>
 */


import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JIBOYE, Oluwagbemiro Olaoluwa <gbenroscience@yahoo.com>
 *
 This class helps manage a database transaction. There is an abstract method
 * called process and 2 fully fledged methods called onSuccess and onError. The
 * onSuccess method defines what should happen to the connection if the process
 * method's implementation completes successfully while onError specifies what
 * should happen to the connection if an error occurs.
 *
 * onSuccess triggers: A commit of all transactions AND a startTransaction to
 * setAutoCommit(true) onError triggers: A rollback of all transactions AND a
 * startTransaction to setAutoCommit(true)
 *
 * The method, process, returns true or false based on whether the transactions were successful
 * or not. If the transactions were successful and so should be committed,
 * return true. If you need to discard them, return false. If your method throws
 * an exception, the {@link TransactionManager} is designed to startTransaction
 * onError and so rollback the transactions and also return your Connection
 * object to its autocommit=true state.
 *
 * IN YOUR IMPLEMENTATION OF process(), ANY PORTION OR LOGICAL BRANCH OF THE
 * METHOD THAT SUGGESTS THAT A ROLLBACK IS NEEDED SHOULD IMMEDIATELY return
 * false;
 * 
 * There are 3 callbacks that can be used to determine the next course of action
 * when the manager completes its transaction.
 * 
 * The first is onCommit(), the second is onRollback(), the third is onRollback(int errorcode)
 * The onCommit callback is guaranteed to be called if the transaction was totally successfull.
 * The onRollback callback is guaranteed to be called if the transaction is detected to have failed in at least one point,
 * either by an uncaught exception or by the user returning a false at any point in the process(conn) method.
 * The overloaded version of onRollback is guaranteed to be called under similar circumstances as the first, but it returns
 * an error-code which the user must have set  before  returning false or before a terminating exception is thrown.
 * 
 * Anyone that wishes to handle generic errors would use onRollback().
 * Only anyone that wants to handle particular errors(introduce granularity in error handling)
 * should use onRollback(int). They MUST then set the error code before returning false.
 * 
 *
 *
 *
 */
public abstract class TransactionManager {
    
    /**
     * The code to track in the rollback callback when something goes wrong
     * to allow the user flexibility when handling errors.
     */
    protected int errorCode;
    
    
    /**
     * No error occurred
     */
    public static final int ERROR_NONE = Integer.MAX_VALUE;

    /**
     * If true, the transaction was committed successfully
     */
    private boolean ok;

    
    public void startTransaction(java.sql.Connection conn) {

        try{
            conn.setAutoCommit(false);

            if (process(conn)) {
                onSuccess(conn);
            } else {
                onError(conn);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            onError(conn);
        }

    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    

    /**
     *
     * @return true if the transactions were committed successfully and false
     * otherwise.
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * Do the database transactions here.
     *
     * @param conn The Connection object
     * @return true if the transaction should be committed; and false otherwise.
     */
    public abstract boolean process(java.sql.Connection conn);

    /**
     * Override to perform completion operations like writing to the client if the commit
     * was successful
     */
    public void onCommit() {
   
    }

    /**
     * Override to perform error operations if the transaction failed. At this point, the manager has already rolled back the transactions.
     * If the database transactions failed or if they succeeded but the call to
     * commit failed.
     */
    public void onRollback() {
 
    }
    
    /**
     * Override to perform error operations if the transaction failed. At this point, the manager has already rolled back the transactions.
     * If the database transactions failed or if they succeeded but the call to
     * commit failed.
     * @param errorCode The error code that caused the transaction to rollback
     */
    public void onRollback(int errorCode) {
 
    }

    private void onError(Connection conn) {
        try {
            conn.rollback();
            ok = false;
            onRollback();
            onRollback(errorCode);
            conn.setAutoCommit(true);
        } catch (Exception ex) {
            Logger.getLogger(TransactionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void onSuccess(Connection conn) {
        try {
            conn.commit();
            ok = true;
            onCommit();
            conn.setAutoCommit(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(TransactionManager.class.getName()).log(Level.SEVERE, null, ex);

        }
    }

}
