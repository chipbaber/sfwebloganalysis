package com.chip.code;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class read_spreadsheets_workshops {
    private final List<sfSession> sessionList = new ArrayList<sfSession>();
    private List<sfSession> g_sessionList = new ArrayList<sfSession>();
    //Buckets for Data Analysis
    private List<sfSession> g_sessionList_customer = new ArrayList<sfSession>();
    private List<sfSession> g_sessionList_anonymous = new ArrayList<sfSession>();
    private List<sfSession> g_sessionList_employees = new ArrayList<sfSession>();

    private static RandomAccessFile access_file;
    private static File logfile;
    private String accel_name="";
    private String output_dir="";



    private read_spreadsheets_workshops() {
        super();
    }

    /*
     Method Clears out the sessionList array for new set of processing.
     */
    private void clearlist(){
        sessionList.clear();
    }

    public void printStats(){
        this.getSessionCount();
        this.uniqueUsers();
        this.getCoreStats();
        this.clearlist();
    }

    private void addToGlobalStats(){
        g_sessionList.addAll(sessionList);
    }

    public void getStatsGlobal(int a, String mes) {
        logger(mes + "(Stats from "+a+" microsites collected/tallied.)");
        //clear the list
        this.clearlist();
        //swap global into working session
        sessionList.addAll(g_sessionList);
        this.getSessionCount();
        this.uniqueUsers();
        this.getCoreStats();
        this.accessByGroup();
        accel_name = "global_json";
    }

    /* Function will take a data bucket and output the number of unique users and core stats like video.
     *
     * */
    private void getStatsGlobal(List<sfSession> data) {
        this.uniqueUsers(data);
        this.getCoreStats(data);
    }

    /**
     * This method is used to read the data's from an excel file.
     * @param fileName - Name of the excel file.
     */
    private void readExcelFile(String fileName, int sheet_number, String accelerate_name, String v_output_dir) {
        try  {
            accel_name=accelerate_name;
            output_dir=v_output_dir;

            //read file and place in POI variable
            // logger(""+accelerate_name+" Accelerate Q2 Stats");

            FileInputStream fileInputStream = new FileInputStream(fileName);
            POIFSFileSystem fsFileSystem = new POIFSFileSystem(fileInputStream);
            HSSFWorkbook workBook = new HSSFWorkbook(fsFileSystem);

            //Reads the first worksheet in the xls file.
            HSSFSheet hssfSheet = workBook.getSheetAt(sheet_number);
            HSSFRow hssfRow;

            Iterator rowIterator = hssfSheet.rowIterator();

            String current_session = "0";
            boolean new_session =false;
            sfSession holder = new sfSession();
            holder.setAccelerate_name(accelerate_name);
            sfAction action_holder =new sfAction();
            int session_counter = 0;

            //for each row in spreadsheet
            while (rowIterator.hasNext()) {
                //read row
                hssfRow = (HSSFRow) rowIterator.next();
                //place in iterator
                Iterator celliterator = hssfRow.cellIterator();

                while (celliterator.hasNext())     {
                    HSSFCell hssfCell = (HSSFCell) celliterator.next();
                    //logger("Cell Column index is: " +hssfCell.getColumnIndex() + " Value is: "+hssfCell.toString());

                    //When looking at Cell 0 perform session check
                    if (hssfCell.getColumnIndex() == 0 ) {
                        if (current_session.equals(hssfCell.toString())) {
                            //logger("No Session Change");
                            new_session=false;
                        }
                        else {
                            //save to arraylist
                            sessionList.add(holder);
                            //reset variables
                            holder =new sfSession();
                            holder.setAccelerate_name(accelerate_name);
                            new_session=true;
                            current_session=hssfCell.toString();
                            session_counter++;
                        }
                    }

                    //When a new session starts handle the first master rows
                    if (new_session) {

                        switch (hssfCell.getColumnIndex()) {
                            case 1: holder.setV_date(hssfCell.toString()); /*logger("Date:"+ hssfCell.toString());*/ break;
                            case 2: holder.setEmailaddress(hssfCell.toString()); /*logger("Email Address:"+ hssfCell.toString());*/ break;
                            case 5: holder.setGroup(hssfCell.toString());/*logger("Group: "+ hssfCell.toString());*/ break;
                            case 6: holder.setBrowser(hssfCell.toString());/*logger("Browser: "+ hssfCell.toString());*/ break;
                            default:
                                // logger("Extra column in .csv file, skipping value.");
                        }
                    }
                    //When not a new session log action details
                    else {
                        /**/
                        switch (hssfCell.getColumnIndex()) {
                            case 2: action_holder.setV_actionTime(hssfCell.getDateCellValue());/*logger("Time:"+ hssfCell.getDateCellValue());*/ break;
                            case 3: action_holder.setAction(hssfCell.toString());/*logger("Action:"+ hssfCell.toString());*/ break;
                            case 4: action_holder.setLink(hssfCell.toString());/*logger("Link: "+ hssfCell.toString());*/ break;
                            default:
                                // logger("Extra column in .csv file, skipping value.");
                        }
                    }

                }

                //add session clicks to holder if not a new session
                if (!new_session) {
                    holder.addAction(action_holder);
                    action_holder =new sfAction();
                }

            } //end while
            //add the last row once the row iterator completes.
            sessionList.add(holder);


        }
        catch (Exception e)  {
            e.printStackTrace();
        }
    }


    /*This method is designed to look at all the data in the spreadsheets and filter out the information that is clearly
     * a bot or error in the program.*/
    private void cleanseData(String[] emails) {
        try {
            logger("Begining Data Cleanse Method");
            logger("------------------------------------------------------------------------");
            logger("Initial Global Session Size is: "+ g_sessionList.size()+"\n");

            logger("Calculate Global Unique Sessions --> Check to Solution Factory");
            logger("------------------------------------------------------------------------");
            this.uniqueUsers(g_sessionList);
            logger("");

            logger("Clearing All Null Session Values");
            logger("------------------------------------------------------------------------");
            this.clearNulls();
            logger("Global Output size is: "+ g_sessionList.size()+"\n");
            logger("Remove Session Emails for: "+ Arrays.toString(emails));
            logger("------------------------------------------------------------------------");
            this.removeGlobalSessionEmails(emails);
            logger("Global Output size is: "+ g_sessionList.size()+"\n");

            logger("Begin Bucketing Data");
            logger("------------------------------------------------------------------------");
            this.createAnalysisBuckets();
            logger("Reporting Buckets Created:\n");
            logger("Employee Sessions: "+ g_sessionList_employees.size()+"\n");
            this.sessionsAnalysis(g_sessionList_employees,"Employee Session Analysis");
            logger("Anonymous Login Sessions: "+ g_sessionList_anonymous.size()+"\n");
            this.sessionsAnalysis(g_sessionList_anonymous,"Anonymous Session Analysis");
            logger("Customers Providing Personal Email Sessions: "+ g_sessionList_customer.size()+"\n");
            this.sessionsAnalysis(g_sessionList_customer,"Anonymous Session Analysis");
            /**/
        }
        catch (Exception e) {
            logger("Error in cleanseData method." + e.toString());
        }
    }

    /*Method removeUserSessions looks at the global sessions and removes unwanted email address's
     */
    public void removeEmployeeSessionEmails(String[] emails){
        //use streams to filter down arraylist
        try {
            List<sfSession> temp  = new ArrayList<sfSession>();
            //loop through and filter each occurence of the string.
            for (String e : emails) {
                temp = g_sessionList_employees.stream()
                        .filter(session -> !e.equals(session.getEmailaddress()))
                        .collect(Collectors.toList());
                //logger("Temp Size after filter: " + temp.size());
            }
            g_sessionList_employees =temp;
        }
        catch (Exception e) {
            logger("Error in removeUserEmployeeSessions method: \n"+e.toString());
        }
    }

    private void removeGlobalSessionEmails(String[] emails){
        //use streams to filter down arraylist
        try {
            List<sfSession> temp  = new ArrayList<sfSession>();
            //loop through and filter each occurence of the string.
            for (String e : emails) {
                temp = g_sessionList.stream()
                        .filter(session -> !e.equals(session.getEmailaddress()))
                        .collect(Collectors.toList());
                //logger("Temp Size after filter: " + temp.size());
            }
            g_sessionList =temp;
        }
        catch (Exception e) {
            logger("Error in removeUserEmployeeSessions method: \n"+e.toString());
        }
    }


    /* Create Data Buckets, one for employees, one for customers who used there email address, one for anonymous
     * accounts.
     *
     * */
    private void createAnalysisBuckets(){
        try {
            /*Build out Employee Click Bucket*/
            g_sessionList_employees = g_sessionList.stream().filter(session -> session.getEmailaddress().endsWith("@oracle.com") ).collect(Collectors.toList());
            /*Build out Anonymous Click Bucket*/
            g_sessionList_anonymous = g_sessionList.stream().filter(session -> session.getEmailaddress().endsWith("@sf_generic_oracle.com") ).collect(Collectors.toList());
            /*Build out Customer Email Bucket*/
            g_sessionList_customer = g_sessionList.stream().filter(session -> !session.getEmailaddress().endsWith("@oracle.com") ).collect(Collectors.toList());
            g_sessionList_customer = g_sessionList_customer.stream().filter(session -> !session.getEmailaddress().endsWith("@sf_generic_oracle.com") ).collect(Collectors.toList());
        }
        catch (Exception e) {
            logger("Error in createAnalysisBuckets() method: \n"+e.toString());
        }
    }
    /*Remove any null values from the .xls sheet before processing with streams.
     *
     * */
    private void clearNulls(){
        try {
            List<sfSession> temp  = new ArrayList<sfSession>();
            for (sfSession session : g_sessionList) {
                if (session.getEmailaddress() != null && !session.getEmailaddress().isEmpty()) {
                    temp.add(session);
                }
            }
            g_sessionList=temp;
        }
        catch (Exception e) {
            logger("Error in clearNulls() method: \n"+e.toString());
        }

    }

    private void sessionsAnalysis(List<sfSession> data, String name){
        try {
            //temp array for known human stats
            List<sfSession> human_corestats =new ArrayList<sfSession>();

            logger("Scoring Model for "+name);
            logger("-------------------------------------------------------------------------------------");
            int points = 0, bot =0, greyBot=0, human=0;

            for (sfSession session : data) {
                List<sfAction> a = session.getActions();
                //for each action we will assign a score to determine the validity log and engagement.
                for (sfAction action : a) {
                    //+1 pt for launch url -- Bots can do this
                    if (action.getAction().startsWith("Launch to URL")) {
                        points++;
                    }
                    //+1 for .json load of control file
                    if (action.getAction().equals("File download : MANAGER") /*&& action.getLink().endsWith("jsonId=REPO_f1201093661_d393560595&pin=cloudnative")*/) {
                        points++;
                    }
                    //page partially loads successfully
                    if (action.getAction().startsWith("Click : Browser Width:")) {
                        points++;
                    }
                    //page fully loads and we have a response time of the load.
                    if (action.getLink().startsWith("Page Load Time")) {
                        points++;
                    }
                    //Add points for navigation clicks via header or keyboard, this has to be User
                    if (action.getAction().equals("Click : Next Button Clicked Viewing Architecture Image")
                            || action.getAction().equals("Click : Previous Button Clicked Viewing Architecture Image")
                            || action.getAction().startsWith("Click : Header Tab Click -")
                            || action.getAction().equals("Click : Video Playing")
                            || action.getAction().equals("Click : Content Tray Opened")
                            || action.getAction().startsWith("Click : Header Tab Click ")
                            || action.getAction().startsWith("Click : Right Key Pressed")
                            || action.getAction().startsWith("Click : Left Key Pressed")
                            || action.getLink().startsWith("Page Load Time is: ")
                    ) {
                        points++;
                    }
                }
                if (points<=2){
                    bot++;
                }
                else if(points>2 && points <5) {
                    greyBot++;
                }
                else if(points>=5) {
                    human++;
                    human_corestats.add(session);
                }
                else {
                    //do nothing
                }
                points =0;
            }
            logger("Bucket 1: BOT/Ping Sessions: "+bot+ "                              Percentage of total Sessions: "+(float)bot/data.size());
            logger("Bucket 2: Maybe BOT/Maybe Human Sessions: "+greyBot+ "                 Percentage of total Sessions: "+(float)greyBot/data.size());
            logger("Bucket 3: Definately Human Sessions: "+human+ "                      Percentage of total Sessions: "+(float)human/data.size());
            logger("|");
            logger("|");
            logger("|");

            logger("|---Bucket 3: Website Usage Stats for "+name);
            logger("    -------------------------------------------------------------------------------------");
            getStatsGlobal(human_corestats);
            logger(" \n");

        }
        catch (Exception e) {
            logger("Error in removeAllOracleEmps() method: \n"+e.toString());
        }
    }

    /*This Method outputs the excel file a valid formatted .json file.
     */
    public void writeJson() {
        try {
            sfSession temp;
            sfAction temp2;
            this.createLogFile();
            this.openLogFile();
            Iterator<sfSession> output = sessionList.iterator();

            //write intro
            writeLogLine("{");
            writeLogLine("\"session\": [");

            while (output.hasNext()) {
                temp =output.next();
                writeLogLine("{");
                writeLogLine("\"accelerate_name\": \""+temp.getAccelerate_name() +"\",");
                writeLogLine("\"date\":\""+temp.getV_date()+"\",");
                writeLogLine("\"emailaddress\":\""+temp.getEmailaddress()+"\",");
                writeLogLine("\"group\":\""+temp.getGroup()+"\",");
                writeLogLine("\"browser\":\""+temp.getBrowser()+"\",");
                writeLogLine("\"clicks\" :[");

                //iterate through Actions for a session
                Iterator<sfAction> output_actions= temp.getActions().iterator();
                while (output_actions.hasNext()) {
                    temp2=output_actions.next();
                    writeLogLine("{");
                    writeLogLine("\"time\":\""+temp2.getV_actionTime()+"\",");
                    writeLogLine("\"action\":\""+temp2.getAction()+"\",");
                    writeLogLine("\"link\":\""+temp2.getLink()+"\"");

                    if ( !output_actions.hasNext()) {
                        writeLogLine("}");
                    }
                    else {
                        writeLogLine("},");
                    }
                }

                writeLogLine("]");

                //if last element no comma in .json
                if ( !output.hasNext()) {
                    writeLogLine("}");
                }
                else {
                    writeLogLine("},");
                }

            }
            writeLogLine(" ] }");


            this.closeLogFile();
        }
        catch (Exception e) {
            this.closeLogFile();
            logger("Error in writeJSON. \n"+e.toString());
        }
    }


    private void getSessionCount(){
        logger("Number of Sessions: "+ sessionList.size());
    }

    /*
     * Find the number of Unique Email Address's in the File.
     */

    private void accessByGroup(){
        List<String> groups = new ArrayList<String>();

        //get all the groups in an array
        for (com.chip.code.sfSession sfSession : sessionList) {
            groups.add(sfSession.getGroup());
        }

        Set<String> unique = new HashSet<String>(groups);
        logger("Session Count by Group:");
        for (String key : unique) {
            logger(key + ": " + Collections.frequency(groups, key));
        }
    }

    private void uniqueUsers(){
        int visitors = 0;

        List<String> email = new ArrayList<String>();

        //get all the groups in an array
        for (com.chip.code.sfSession sfSession : sessionList) {
            email.add(sfSession.getEmailaddress());
        }

        Set<String> unique = new HashSet<String>(email);

        for (String key : unique) {
            Collections.frequency(email, key);
            //logger(key + ": " + Collections.frequency(email, key));
            visitors++;
        }

        logger("Unique visitors: "+visitors);
    }

    private void uniqueUsers(List<sfSession> data){
        int visitors = 0;
        List<String> email = new ArrayList<String>();
        //get all the groups in an array
        for (sfSession datum : data) {
            email.add(datum.getEmailaddress());
        }

        Set<String> unique = new HashSet<String>(email);
        for (String key : unique) {
            Collections.frequency(email, key);
            visitors++;
        }

        logger("    Unique Sessions: "+visitors);
    }


    private void getCoreStats(){
        sfSession temp;
        sfAction temp2;
        int archViews =0;
        int video=0;
        int completeVideo=0;
        double watch_time=0.0;

        //get all the groups in an array
        for (com.chip.code.sfSession sfSession : sessionList) {
            temp = sfSession;
            for (com.chip.code.sfAction sfAction : (Iterable<sfAction>) temp.getActions()) {
                temp2 = sfAction;

                if (temp2.getAction().toLowerCase().contains("viewing architecture image")) {
                    archViews++;
                } else if (temp2.getAction().toLowerCase().contains("video playing")) {
                    video++;
                } else if (temp2.getAction().toLowerCase().contains("video ended")) {
                    completeVideo++;
                } else if (temp2.getAction().toLowerCase().contains("timecode")) {
                    watch_time = watch_time + Double.parseDouble(temp2.getLink());
                } else {

                }
            }
        }
        logger("Workshop Architecture Frame Views: "+archViews);
        logger("Videos (Story/Prototype) Viewed: "+video);
        logger("Total Watch Time: "+TimeUnit.SECONDS.toMinutes((long)watch_time)+ " minutes. ");
        logger("Videos Watched to Completion: "+completeVideo+ "("+(float)completeVideo/video+"%)");
    }

    private void getCoreStats(List<sfSession> data){
        sfSession temp;
        sfAction temp2;
        int archViews =0;
        int video=0;
        int completeVideo=0;
        double watch_time=0.0;

        //get all the groups in an array
        for (sfSession datum : data) {
            temp = datum;
            for (com.chip.code.sfAction sfAction : (Iterable<sfAction>) temp.getActions()) {
                temp2 = sfAction;

                if (temp2.getAction().toLowerCase().contains("viewing architecture image")) {
                    archViews++;
                } else if (temp2.getAction().toLowerCase().startsWith("click : header tab click")) {
                    archViews++;
                } else if (temp2.getAction().toLowerCase().contains("video playing")) {
                    video++;
                } else if (temp2.getAction().toLowerCase().contains("video ended")) {
                    completeVideo++;
                } else if (temp2.getAction().toLowerCase().contains("timecode")) {
                    watch_time = watch_time + Double.parseDouble(temp2.getLink());
                } else {

                }
            }
        }
        logger("    Workshop Architecture Frame Views: "+archViews);
        logger("    Videos (Story/Prototype) Viewed: "+video);
        logger("    Total Watch Time: "+TimeUnit.SECONDS.toMinutes((long)watch_time)+ " minutes. ");
        logger("    Videos Watched to Completion: "+completeVideo+ "("+(float)completeVideo/video+"%)");
    }


    /*
     * Attempt to write a line to the logfile.
     */

    private void writeLogLine(String a) {
        try {
            access_file.writeBytes(a+"\n");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Method openLogFile() opens access to the logfile while the program runs.
     */
    private void openLogFile() {
        try {
            access_file  = new RandomAccessFile(logfile, "rwd");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Method openLogFile() close access to the logfile while the program runs.
     */
    private void closeLogFile() {
        try {
            logger("Log file Closed.");
            access_file.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Create a logfile to store logger comments.
     */
    private void createLogFile() {
        logfile = new File(output_dir+accel_name+".json");
        try {
            logfile.createNewFile();
            System.out.println("Log file creation in progress at "+output_dir+ accel_name+".json");
        } catch (IOException e) {
            System.out.println("Error inside the createLofFile method.");
            e.printStackTrace();
        }
    }


    private static void logger(String strMessage) {
        //  System.out.println(now()+" " + strMessage);
        System.out.println(" "+strMessage);
    }

    private static void log(String strMessage) {
        System.out.print(strMessage);
    }

    private static String now() {
        String DATE_FORMAT_NOW = "MM/dd/yy H:mm:ss:SSS";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }


    public static void main(String[] args) {
        String filepath ="C:\\temp\\sf_rpts\\";
        String json_path="C:\\temp\\sf_rpts\\json\\";
        String filename ="";
        int file_count =0;

        /*Read all the .xls files in a directory.*/
        File folder = new File(filepath);
        read_spreadsheets_workshops a = new read_spreadsheets_workshops();
        File[] listOfFiles = folder.listFiles();
        try {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    /*for each file*/
                    filename = listOfFile.getName().substring(0, listOfFile.getName().length() - 4);
                    filename = Character.toUpperCase(filename.charAt(0)) + filename.substring(1);
                    a.readExcelFile(filepath + listOfFile.getName(), 0, filename, json_path);
                    a.addToGlobalStats();
                    // a.printStats();
                    // System.out.println("\n\n");
                    logger("Data Collected from:  " + listOfFile.getName() + "\n");
                    file_count++;
                } else if (listOfFile.isDirectory()) {
                    System.out.println("Directory " + listOfFile.getName());
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error reading the directory of files:" + e.toString());
        }
        /*Begin Cleanse*/
        String[] filterEmails = {"frank.baber@oracle.com","not_found"};
        a.cleanseData(filterEmails);


        //output global stats
          a.getStatsGlobal(file_count,"Workshop Stats Distribution");
         a.writeJson();
          a.accessByGroup();
        System.exit(0);
    }
}
