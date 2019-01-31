# transactionmanager
TransactionManager is a simple Java class that is useful for managing database transactions.


A <a href="https://en.wikipedia.org/wiki/Database_transaction">Database transaction</a> represents a single unit of sql logic or work, sometimes made up of multiple operations(i.e sql statements) We usually want to ensure that once the transaction completes that the full intended logic, either executes or is discarded, causing the database to retain its original state.

<code>TransactionManager.java</code> is a class that helps developers with this. 

1. It basically intercepts a java.sql.Connection instance and makes it non-auto-commitable. Basically this means that any sql statement run on this connection is not automatically commited by the underlting database.

2. It provides a <code>process</code> method within which a programmer can define the logic of their sql-operations. Multiple statements defined here are run without being committed to the database.

The <code>process</code> method returns a boolean value and if at any stage in the method, the developer decides to return false, then the statements run up to that point will never be committed. Else, the statements will be committed.

It also allows the programmer to set various error codes before returning false. 

You have to implement(you may leave them empty) the default methods <code>onCommit</code> and <code>onRollback(int errorCode)</code>
defined in <code>TransactionManager.java</code>

These methods are called immediately by <code>TransactionManager.java</code> once the transaction has been committed or discarded.

If there are any operations that you want to perform once your transaction is successfully concluded, you specify them in </code>onCommit</code>. This may include sending some JSON information to a client or redirecting to some website.

If you would like to take actions based on some error during the transaction for which you set an error-code, define the error operations in <code>onRollback(int errorCode)</code> The errorCode parameter is the error you set in the 
<code>process()</code> method when the error happened. This may include sending some JSON information to a client or redirecting to some website.

Here is some sample code:


```
   TransactionManager manager = new TransactionManager() {
   
 
 /**
  * Your sql transactions go here. 
  * Return false if they throw any error. Return true if all is well.
  * To handle various errors, set an error code for any error that occurrs.
  * The onRollback(int error) callback will allow you to handle that error
  * specifically later on with a simple switch or if
  */
 @Override
 public boolean process(Connection conn) {

   
   }
   
      @Override
       public void onCommit() {
                        super.onCommit(); 
                 sendContent(response, succResp);
             }
             
   @Override
   
   public void onRollback(int errorCode) {
                        super.onRollback(errorCode); 
   }
   
   }
   
   
    manager.startTransaction(conn); 
    
```
   
   
 <b> Now for a simple example: </b>
 

```
   
   TransactionManager manager = new TransactionManager() {

                    JSONObject succResp = new JSONObject();
                    JSONObject errorResp = new JSONObject();
 
                    @Override
                    public boolean process(Connection conn) {

                
                        boolean deletedTripHasReturnTrip = returnTripId != null && !returnTripId.isEmpty();

                        TripStore store = new TripStore(conn);
                        TripStore main = store.getTrip(rideId);

                        boolean deletedTripIsReturnTrip = returnTrip;

                 
                        String deleteSql = "UPDATE " + TripStore.FULL_TABLE_NAME + " SET " + TripStore.Columns.CANCELED + " = ? "
                                + " WHERE " + TripStore.Columns.DRIVER_ID + " = ? AND " + TripStore.Columns.RIDE_ID + " = ? ";

                         String updateSql_1 = "UPDATE " + RideStore.FULL_TABLE_NAME + " SET " + TripStore.Columns.RETURN_TRIP + " = ? "
                                + " WHERE " + TripStore.Columns.DRIVER_ID + " = ? AND " + TripStore.Columns.RIDE_ID + " = ?";

                   
                        String updateSql_2 = "UPDATE " + TripStore.FULL_TABLE_NAME + " SET " + TripStore.Columns.RETURN_TRIP_AVAILABLE + " = ? , "
                                + TripStore.Columns.RETURN_TRIP_RIDE_ID + " = ? "
                                + " WHERE " + TripStore.Columns.DRIVER_ID + " = ? AND " + TripStore.Columns.RIDE_ID + " = ?";

                        try (PreparedStatement st = conn.prepareStatement(deleteSql)) {

                            st.setBoolean(1, true);
                            st.setString(2, driverId);
                            st.setString(3, rideId);
                            int deletedRows = st.executeUpdate();

                            if (deletedRows > 0) {

                                if (deletedTripIsReturnTrip) {
                                    String parentId = store.getRideIDByReturnTripId(rideId, driverId);
                                    System.out.println("parent-id: " + parentId);
                                    try (PreparedStatement st1 = conn.prepareStatement(updateSql_2)) {

                                        st1.setBoolean(1, false);
                                        st1.setString(2, "");
                                        st1.setString(3, driverId);
                                        st1.setString(4, parentId);

                                        int updatedRows = st1.executeUpdate();
                                        
                                        boolean updated = updatedRows > 0;

                                        if (updated) {
                                            succResp.put(STATUS, StatusCodes.SUCCESS);
                                            succResp.put(MESSAGE, "You just canceled your ride.");
                                            succResp.put(TIME, new Date().getTime()); 
                                        } else {
                                            errorResp.put(STATUS, StatusCodes.NOT_FOUND);
                                            errorResp.put(MESSAGE, "The trip you are canceling is a return trip. But we could not find the main trip.\n Cancel Failed!");
                                            errorResp.put(TIME, new Date().getTime()); 
                                        }

                                        return updated;
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        errorResp.put(STATUS, StatusCodes.SERVER_ERROR);
                                        errorResp.put(MESSAGE, "Could not find the return trip");
                                        errorResp.put(TIME, new Date().getTime());
                                        return false;

                                    }
                                }
                                if (deletedTripHasReturnTrip) {

                                    try (PreparedStatement st1 = conn.prepareStatement(updateSql_1)) {

                                        st1.setBoolean(1, false);
                                        st1.setString(2, driverId);
                                        st1.setString(3, returnTripId);

                                        int updatedRows = st1.executeUpdate();
                                        
                                        boolean updated = updatedRows > 0;
                                        if(updated){
                                        succResp.put(STATUS, StatusCodes.SUCCESS);
                                        succResp.put(MESSAGE, "You just canceled your ride.");
                                        succResp.put(TIME, new Date().getTime());  
                                         
                                        }else{
                                        errorResp.put(STATUS, StatusCodes.SERVER_ERROR);
                                        errorResp.put(MESSAGE, "We could not find the return trip");
                                        errorResp.put(TIME, new Date().getTime());     
                                        }

                                    return updated;
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        errorResp.put(MESSAGE, "Server busy... please try again.");
                                        errorResp.put(TIME, new Date().getTime());
                                        errorResp.put(STATUS, StatusCodes.SERVER_ERROR);
                                        
                                        errorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                                        
                                        return false;
                                    }

                                } else {
                                    succResp.put(STATUS, StatusCodes.SUCCESS);
                                    succResp.put(MESSAGE, "You just canceled your ride.");
                                    succResp.put(TIME, new Date().getTime());
                                    
                                    return true;
                                }

                            } else {
                                errorResp.put(STATUS, StatusCodes.SERVER_ERROR);
                                errorResp.put(MESSAGE, "This ride does not exist.");
                                errorResp.put(TIME, new Date().getTime());
                                errorCode = HttpServletResponse.SC_BAD_REQUEST;

                                return false;
                            }

                        } catch (SQLException exception) {
                            errorResp.put(MESSAGE, "Server busy... please try again.");
                            errorResp.put(TIME, new Date().getTime());
                            errorResp.put(STATUS, StatusCodes.SERVER_ERROR);

                            errorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;  
                            exception.printStackTrace();
                        return false;
                        } catch (Exception e) {
                            errorResp.put(STATUS, StatusCodes.FAILURE);
                            errorResp.put(MESSAGE, "Some error occurred");
                            errorResp.put(TIME, new Date().getTime());
                            errorCode = HttpServletResponse.SC_BAD_REQUEST; 
                            e.printStackTrace();
                        return false;
                        }

                    }

                    @Override
                    public void onCommit() {
                        super.onCommit(); //To change body of generated methods, choose Tools | Templates.
                        
                        String msg = succResp.optString(KeyConstants.MESSAGE);
                        msg = msg.concat("\n").concat("Your passengers will be refunded their monies and you will be penalized for their service fees.");
                        
                        sendContent(response, succResp);
                    }

                    @Override
                    public void onRollback(int errorCode) {
                        super.onRollback(errorCode); //To change body of generated methods, choose Tools | Templates.
                        sendError(response, errorCode, errorResp);
                    }

                };
                
                /*Get the connection object from a pool or create it.*/
                manager.startTransaction(conn);
                
```








