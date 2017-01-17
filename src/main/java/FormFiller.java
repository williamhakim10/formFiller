import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.printing.PDFPageable;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.Sides;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;


/**
 * Created by William Hakim on 1/17/2016.
 * States: AZ, CA, TX, WA
 * Ally fully functional
 * Chase partially functional
 */

public class FormFiller {

    private static String outputDir;

    private static boolean print = false;

    private static DecimalFormat moneyFormatter = new DecimalFormat("#,##0.00");

    private static void negativeEquity(HashMap dealerTrackData, double downPayment, String buyerState) {
        Double downPaymentPos = downPayment * -1.;
        dealerTrackData.put("Text56", moneyFormatter.format(downPaymentPos));
        dealerTrackData.put("Text98", "Dealer Payoff");

        if (dealerTrackData.get("buyerState").equals("AZ") || dealerTrackData.get("buyerState").equals("CA")) {
            dealerTrackData.put("Text60", moneyFormatter.format(Double.parseDouble(dealerTrackData.get("Text60").toString().
                    replace(",", "")) + downPaymentPos));

            if (buyerState.equals("CA")) {
                dealerTrackData.put("Text68", moneyFormatter.format(Double.parseDouble(dealerTrackData.get("Text68").
                        toString().replace(",", "")) + downPaymentPos));
            }
        }

        dealerTrackData.put("Text19", "0.00");
        dealerTrackData.put("Text20", dealerTrackData.get("Text18"));

    }

    private static String makeOutputDir(HashMap dealerTrackData) {

        String newDirectory = dealerTrackData.get("Text3") + " " + dealerTrackData.get("fullName");
        File dir = new File(newDirectory);
        dir.mkdir();
        return newDirectory;
    }

    private static void printPdf(PDDocument pdfTemplate, HashMap dealerTrackData, String inputFileName, int copies) {

        try {

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(pdfTemplate));

            // Print out of oversize tray for oversize contract - beta - not sure if this works!
            if (!dealerTrackData.get("buyerState").equals("CA") && inputFileName.contains("Purchase Contract")) {
                PrintRequestAttributeSet pset = new HashPrintRequestAttributeSet();
                pset.add(Sides.DUPLEX);

                Media[] supportedMedia = (Media[]) job.getPrintService().getSupportedAttributeValues(Media.class, null, null);
                for (Media m : supportedMedia) {
                    if (m.toString().equals("Tray 5")) {
                        pset.add(m);
                        break;
                    }
                }
                for (int i = 0; i < copies; i++)
                    job.print(pset);
                return;
            }
            for (int i = 0; i < copies; i++)
                job.print();
        }
        catch (PrinterException e) {
            e.printStackTrace();
        }
    }

    private static void fillPdf(HashMap dealerTrackData, String inputFileName, String outputDir, String outputFormType) {
        try {
            PDDocument pdfTemplate = PDDocument.load(new File(inputFileName));

            PDDocumentCatalog docCatalog = pdfTemplate.getDocumentCatalog();
            PDAcroForm acroForm = docCatalog.getAcroForm();

            List<PDField> fieldList = acroForm.getFields();

            String[] fieldArray = new String[fieldList.size()];
            int i = 0;
            for (PDField sField : fieldList) {
                fieldArray[i] = sField.getFullyQualifiedName();
                i++;
            }

            for (String f : fieldArray) {
                PDField field = acroForm.getField(f);
                String value = (String) dealerTrackData.get(f);
                if (value != null) {
                    try {
                        field.setValue(value);
                    }
                    catch (IllegalArgumentException e) {
                        System.err.println("Could not insert: " + f + ".");
                    }
                }
            }


            pdfTemplate.save(outputDir + "/" + dealerTrackData.get("fullName") + " " + outputFormType + ".pdf");


            // printing - need to look into the long form stuff!
            if (print && !inputFileName.contains("Title Guarantee"))
                printPdf(pdfTemplate, dealerTrackData, inputFileName, inputFileName.contains("Purchase Contract") ? 2 : 1);

            pdfTemplate.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static File getLatestFileFromDir(String dirPath){
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0)
            return null;

        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    private static void getAllyCreditInfo(JavascriptExecutor jse, HashMap dealerTrackData, boolean cosigner) {
        try {
            // Dealer Number & Beepi Address - Based on State
            switch((String) dealerTrackData.get("buyerState")) {
                case "CA":
                    dealerTrackData.put("Dealer Number", "84834");
                    dealerTrackData.put("BeepiAddress", "44380 Old Warm Springs Blvd");
                    dealerTrackData.put("BeepiCity", "Fremont");
                    dealerTrackData.put("BeepiState", "CA");
                    dealerTrackData.put("BeepiZip", "94538");
                    break;
                case "AZ":
                    dealerTrackData.put("Dealer Number", "L00013486");
                    dealerTrackData.put("BeepiAddress", "2915 E. Washington Street Unit 104");
                    dealerTrackData.put("BeepiCity", "Phoenix");
                    dealerTrackData.put("BeepiState", "AZ");
                    dealerTrackData.put("BeepiZip", "85034");
                    break;
                case "TX":
                    dealerTrackData.put("Dealer Number", "P129030");
                    dealerTrackData.put("BeepiAddress", "3000 W. Commerce Suite 120");
                    dealerTrackData.put("BeepiCity", "Dallas");
                    dealerTrackData.put("BeepiState", "TX");
                    dealerTrackData.put("BeepiZip", "75212");
                    break;
                case "WA":
                    dealerTrackData.put("Dealer Number", "0578");
                    dealerTrackData.put("BeepiAddress", "18235 Olympic Avenue South");
                    dealerTrackData.put("BeepiCity", "Tukwila");
                    dealerTrackData.put("BeepiState", "WA");
                    dealerTrackData.put("BeepiZip", "98188");
            }

            // Date of Birth
            String dateOfBirthMonth = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_birth_month').value");
            String dateOfBirthDay = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_birth_day').value");
            String dateOfBirthYear = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_birth_year').value");
            dealerTrackData.put("Date of Birth", dateOfBirthMonth + '/' + dateOfBirthDay + '/' + dateOfBirthYear);


            // Home Phone
            String homePhone0 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_home_phone1').value");
            String homePhone1 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_home_phone2').value");
            String homePhone2 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_home_phone3').value");

            dealerTrackData.put("fill_20", homePhone0);
            dealerTrackData.put("fill_21", homePhone1 + "-" + homePhone2);
            dealerTrackData.put("Home (or business) Phone Number", "(" + homePhone0 + ")" + " " + homePhone1 + "-" + homePhone2);

            // Email
            String email = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_email').value");
            dealerTrackData.put("E-Mail Address", email);

            // Time at Present Address
            String yearsResident = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_years_at_address').value");
            String monthsResident = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_months_at_address').value");
            dealerTrackData.put("yph", yearsResident);
            dealerTrackData.put("mph", monthsResident);

            // Residence Type - to be handled as radio buttons
            String resType = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_ownership_type').value");
            switch (resType) {
                case "N": dealerTrackData.put("Residence Type", "Owns Outright");
                    break;
                case "O": dealerTrackData.put("Residence Type", "Buying");
                    break;
                case "R": dealerTrackData.put("Residence Type", "RentingLeasing");
                    break;
                case "P": dealerTrackData.put("Residence Type", "Family");
                    break;
                case "X": dealerTrackData.put("Residence Type", "Other");
                    break;
                case "M": dealerTrackData.put("Residence Type", "Other");
                    break;
            }

            // Monthly Rent/Mortgage
            String monthlyRentMtgPayment = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_mortgage_rent').value");
            dealerTrackData.put("Monthly Rent/Mortgage Payment", monthlyRentMtgPayment);

            // Present Employer
            String presentEmployer = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_employer_bus').value");
            dealerTrackData.put("Present Employer", presentEmployer);

            // Job Title
            String presentJobTitle = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_occupation').value");
            dealerTrackData.put("Job Title", presentJobTitle);

            // Employer Phone
            String workPhone0 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_bus_phone1').value");
            String workPhone1 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_bus_phone2').value");
            String workPhone2 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_bus_phone3').value");
            dealerTrackData.put("Employer Phone Number", "(" + workPhone0 + ")" + " " + workPhone1 + "-" + workPhone2);

            // Time at Present Job
            String yearsEmployed = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_years_employed').value");
            String monthsEmployed = (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_months_employed').value");
            dealerTrackData.put("ypj", yearsEmployed);
            dealerTrackData.put("mpj", monthsEmployed);

            // Gross Income
            String grossIncome = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_salary').value");
            Double grossIncomeDouble = grossIncome.equals("") ? 0. : Double.parseDouble(grossIncome);
            dealerTrackData.put("Gross Income", moneyFormatter.format(grossIncomeDouble));

            // Term
            String carTermString = (String) dealerTrackData.get("Number Payments 4");
            dealerTrackData.put("Term", dealerTrackData.get("buyerState").equals("CA") ?
                    String.valueOf(Integer.parseInt(carTermString) + 1) : carTermString);

            // Mileage
            String carMileage = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_mileage').value");
            dealerTrackData.put("Mileage", carMileage);


            // Driver's License
            String driversLicense = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                    ".body.childNodes[2].contentDocument.getElementById('app_driver_license_no').value");
            dealerTrackData.put("driversLicense", driversLicense);

            if (cosigner) {
                // Cosigner Date of Birth
                String coAppDateOfBirthMonth = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_birth_month').value");
                String coAppDateOfBirthDay = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_birth_day').value");
                String coAppDateOfBirthYear = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_birth_year').value");
                dealerTrackData.put("Date of Birth2", coAppDateOfBirthMonth + '/' + coAppDateOfBirthDay + '/' + coAppDateOfBirthYear);

                // Cosigner Home Phone
                String coAppHomePhone0 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_home_phone1').value");
                String coAppHomePhone1 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_home_phone2').value");
                String coAppHomePhone2 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_home_phone3').value");
                dealerTrackData.put("CoApp Phone Number", "(" + coAppHomePhone0 + ")" + " " + coAppHomePhone1 +
                        "-" + coAppHomePhone2);

                // Cosigner Email
                String coAppEmail = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_email').value");
                dealerTrackData.put("E-Mail Address2", email);

                // Cosigner Time at Present Address
                String coAppYearsResident = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_years_at_address').value");
                String coAppMonthsResident = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_months_at_address').value");
                dealerTrackData.put("cyph", coAppYearsResident);
                dealerTrackData.put("cmph", coAppMonthsResident);

                // Cosigner Residence Type - to be handled as radio buttons
                String coAppResType = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document" +
                        ".body.childNodes[2].contentDocument.getElementById('app_co_ownership_type').value");
                switch (coAppResType) {
                    case "N": dealerTrackData.put("CoApp Residence Type", "Owns Outright");
                        break;
                    case "O": dealerTrackData.put("CoApp Residence Type", "Buying");
                        break;
                    case "R": dealerTrackData.put("CoApp Residence Type", "RentingLeasing");
                        break;
                    case "P": dealerTrackData.put("CoApp Residence Type", "Family");
                        break;
                    case "X": dealerTrackData.put("CoApp Residence Type", "Other");
                        break;
                    case "M": dealerTrackData.put("CoApp Residence Type", "Other");
                        break;
                }

                // Cosigner Monthly Rent/Mortgage
                String coAppMonthlyRentMtgPayment = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_mortgage_rent').value");
                dealerTrackData.put("CoApp RentMortgage", coAppMonthlyRentMtgPayment);

                // Cosigner Present Employer
                String coAppPresentEmployer = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_employer_bus').value");
                dealerTrackData.put("CoApp Present Employer", coAppPresentEmployer);

                // Cosigner Job Title
                String coAppPresentJobTitle = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_occupation').value");
                dealerTrackData.put("CoApp Job Title", coAppPresentJobTitle);

                // Cosigner Employer Phone
                String coAppWorkPhone0 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_bus_phone1').value");
                String coAppWorkPhone1 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_bus_phone2').value");
                String coAppWorkPhone2 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_bus_phone3').value");
                dealerTrackData.put("CoApp Employer Phone Number", "(" + coAppWorkPhone0 + ")" + " " + coAppWorkPhone1 +
                        "-" + coAppWorkPhone2);

                // Cosigner Time at Present Job
                String coAppYearsEmployed = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById" +
                        "('app_co_years_employed').value");
                String coAppMonthsEmployed = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById" +
                        "('app_co_months_employed').value");
                dealerTrackData.put("cypj", coAppYearsEmployed);
                dealerTrackData.put("cmpj", coAppMonthsEmployed);

                // Cosigner Gross Income
                String coAppGrossIncome = (String) jse.executeScript("return document.getElementById('iFrm')." +
                        "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_co_salary').value");
                Double coAppGrossIncomeDouble = coAppGrossIncome.equals("") ? 0. : Double.parseDouble(coAppGrossIncome);
                dealerTrackData.put("Gross Income2", moneyFormatter.format(coAppGrossIncomeDouble));

                // Set Cosigner Income to Monthly
                dealerTrackData.put("CoApp Income Type", "Monthly");

            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getChaseCreditInfo(JavascriptExecutor jse, HashMap dealerTrackData, WebDriver driver) throws InterruptedException, IOException {

        // Home Phone
        String homePhone0 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_home_phone1').value");
        String homePhone1 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_home_phone2').value");
        String homePhone2 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_home_phone3').value");

        dealerTrackData.put("Home (or business) Phone Number", "(" + homePhone0 + ")" + " " + homePhone1 + "-" + homePhone2);

        // Business Phone
        String workPhone0 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_bus_phone1').value");
        String workPhone1 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_bus_phone2').value");
        String workPhone2 = (String) jse.executeScript("return document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_bus_phone3').value");

        dealerTrackData.put("workPhone", "(" + workPhone0 + ")" + " " + workPhone1 + "-" + workPhone2);

        jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                "getElementsByName('cmdPrint')[0].click()");


        Thread.sleep(4000);
        File chaseCreditApplication;
        String downloadDir = System.getProperty("os.name").toLowerCase().contains("win") ? "C:/Users/" +
                System.getProperty("user.name") + "/Downloads/" : "/Users/" + System.getProperty("user.name") + "/Downloads/";
        do {
            chaseCreditApplication = getLatestFileFromDir(downloadDir);
            Thread.sleep(1000);
        }
        while (chaseCreditApplication == null || System.currentTimeMillis() > chaseCreditApplication.lastModified() + 4000);

        PDDocument creditAppTemplate = PDDocument.load(chaseCreditApplication);
        outputDir = makeOutputDir(dealerTrackData);
        creditAppTemplate.save(outputDir + "/" + dealerTrackData.get("fullName") + " " + "Chase Credit App.pdf");

        if (print)
            printPdf(creditAppTemplate, dealerTrackData, "Chase Credit Application", 1);
        creditAppTemplate.close();

    }

    private static WebDriver getNewFirefoxDriver() {
        FirefoxProfile profile = new FirefoxProfile();

        profile.setPreference("browser.download.folderList", 1);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk","application/pdf");

        profile.setPreference("pdfjs.disabled",true);

        profile.setPreference("plugin.scan.plid.all",false);
        profile.setPreference("plugin.scan.Acrobat","90.0");
        profile.setPreference("plugin.disable_full_page_plugin_for_types","application/pdf");

        return new FirefoxDriver(profile);
    }

    private static void recalculateChaseStructure(JavascriptExecutor jse) {
        // Recalculate the Base Price
        jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                "contentDocument.getElementsByName('app_price')[0].value -= (+(document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementsByName('app_sales_tax')[0]." +
                "value) + +(document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                "contentDocument.getElementsByName('app_ttl')[0].value) + +(document.getElementById('iFrm')." +
                "contentWindow.document.body.childNodes[2].contentDocument.getElementsByName('app_other_finance_fees')" +
                "[0].value))");

        // Update the Amount Financed
        jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                "contentDocument.getElementsByName('app_unpaid_balance')[0].onchange()");
    }

    private static double getTradeInDetails(JavascriptExecutor jse, HashMap dealerTrackData) throws WebDriverException, NumberFormatException {
        String tradeInPrice = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementsByName('app_net_trade')[0].value");
        double tradeInPriceDouble = Double.parseDouble(tradeInPrice);

        String tradeInPriceFormatted = (tradeInPriceDouble < 0) ? "(" + moneyFormatter.format(tradeInPriceDouble * -1) + ")" :
                moneyFormatter.format(tradeInPriceDouble);

        dealerTrackData.put("Text69", tradeInPriceFormatted);
        dealerTrackData.put("Text71", tradeInPriceFormatted);

        String tradeInYear = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementsByName('app_trade_auto_yearList')[0].value");

        dealerTrackData.put("Text103", tradeInYear);

        String tradeInMake = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementsByName('app_trade_auto_makeList')[0].value");

        dealerTrackData.put("Text104", tradeInMake);

        String tradeInModel = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementsByName('app_trade_auto_modelList')[0].value");

        dealerTrackData.put("Text105", tradeInModel);

        return tradeInPriceDouble;
    }

    private static void californiaContract(String[] args, JavascriptExecutor jse, HashMap dealerTrackData, double carBasePriceDouble,
        double carTaxesDouble, double serviceContractDouble, double regFeesDouble, double documentFeeDouble, double totalOfPayments) {

        // Total Cash Price
        double totalCashPrice = carTaxesDouble + carBasePriceDouble + serviceContractDouble + documentFeeDouble;
        String totalCashPriceFormatted = moneyFormatter.format(totalCashPrice);

        dealerTrackData.put("Text60", totalCashPriceFormatted);

        // Subtotal
        double subtotal = totalCashPrice + regFeesDouble;
        String subtotalFormatted = moneyFormatter.format(subtotal);

        dealerTrackData.put("Text68", subtotalFormatted);

        // Trade-In
        double tradeInPriceDouble = 0;

        try {
            tradeInPriceDouble = getTradeInDetails(jse, dealerTrackData);
        }
        catch (WebDriverException | NumberFormatException e) {}

        // Get Cash Down (excluding trade)
        double cashDownNoTrade = Double.parseDouble((String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementsByName('app_cash_down_amt')[0].value"));
        String cashDownFormatted = moneyFormatter.format(cashDownNoTrade);

        // If less than $500 total in cash, put it all in Reservation Deposit
        if (cashDownNoTrade <= 500) {
            dealerTrackData.put("Text74", cashDownFormatted);
        }
        // If more than $500, put $500 in Reservation Deposit and the rest in Cash
        else if (cashDownNoTrade > 500) {
            dealerTrackData.put("Text74", "500.00");
            dealerTrackData.put("Text75", moneyFormatter.format(cashDownNoTrade - 500.));
        }

        // Store total cash payment for Ally credit app
        if (args[1].equals("Ally"))
            dealerTrackData.put("Cash Downpayment", cashDownFormatted);

        // Sum cash down and net trade
        double cashDownTotal = cashDownNoTrade + tradeInPriceDouble;

        // If greater than zero, put sum into Total Downpayment
        if (cashDownTotal >= 0) {
            dealerTrackData.put("Text76", moneyFormatter.format(cashDownTotal));

            // Then fill down payment and total sale price on page 1
            dealerTrackData.put("Text19", moneyFormatter.format(cashDownTotal));
            dealerTrackData.put("Text20", moneyFormatter.format(cashDownTotal + totalOfPayments));
        }
        // If not, put down payment at zero and handle negative equity on trade
        else {
            negativeEquity(dealerTrackData, cashDownTotal, "CA");
            dealerTrackData.put("Text76", "0.00");
        }

    }

    private static void arizonaTexasWashingtonContract(String[] args, JavascriptExecutor jse, HashMap dealerTrackData,
        double carBasePriceDouble, double carTaxesDouble, double serviceContractDouble, double regFeesDouble,
        double documentFeeDouble, double totalOfPayments) {

        // Total Cash Price
        double totalCashPrice = dealerTrackData.get("buyerState").equals("TX") ? carBasePriceDouble + carTaxesDouble :
                carBasePriceDouble + carTaxesDouble + documentFeeDouble;
        dealerTrackData.put("Text60", moneyFormatter.format(totalCashPrice));

        double tradeInPriceDouble = 0;

        try {
            tradeInPriceDouble = getTradeInDetails(jse, dealerTrackData);

            if (dealerTrackData.get("buyerState").equals("AZ") || dealerTrackData.get("buyerState").equals("WA")) {
                dealerTrackData.put("TradeIn", dealerTrackData.get("Text103") + " " + dealerTrackData.get("Text104") + " " +
                        dealerTrackData.get("Text105"));
            }
        }
        catch (WebDriverException | NumberFormatException e) {}

        // Get Cash Down (excluding trade)
        double cashDownNoTrade = Double.parseDouble((String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementsByName('app_cash_down_amt')[0].value"));
        String cashDownFormatted = moneyFormatter.format(cashDownNoTrade);

        // If less than $500 total in cash, put it all in Reservation Deposit
        if (cashDownNoTrade <= 500) {
            dealerTrackData.put("Text74", cashDownFormatted);
        }
        // If more than $500, put $500 in Reservation Deposit and the rest in Cash
        else if (cashDownNoTrade > 500) {
            dealerTrackData.put("Text74", "500.00");
            dealerTrackData.put("Text75", moneyFormatter.format(cashDownNoTrade - 500.));
        }

        // Store total cash payment for Ally credit app
        if (args[1].equals("Ally"))
            dealerTrackData.put("Cash Downpayment", cashDownFormatted);

        // Sum cash down and net trade
        double cashDownTotal = cashDownNoTrade + tradeInPriceDouble;

        // If greater than zero, put sum into Total Downpayment
        if (cashDownTotal >= 0)  {
            dealerTrackData.put("Text76", moneyFormatter.format(cashDownTotal));

            // Then register lien payoff as 0
            dealerTrackData.put("Text56", moneyFormatter.format(0));

            // Then fill down payment and total sale price above
            dealerTrackData.put("Text19", moneyFormatter.format(cashDownTotal));
            dealerTrackData.put("Text20", moneyFormatter.format(cashDownTotal + totalOfPayments));
        }
        // If not, put down payment at zero and handle negative equity on trade
        else {
            negativeEquity(dealerTrackData, cashDownTotal, (String) dealerTrackData.get("buyerState"));
            cashDownTotal = 0.;
            dealerTrackData.put("Text76", moneyFormatter.format(cashDownTotal));
        }

        // Fill Unpaid Balance of Cash Price
        dealerTrackData.put("undefined_24", moneyFormatter.format(Double.parseDouble(dealerTrackData.get("Text60").
            toString().replace(",", "")) - cashDownTotal));

        // Fill Total Other Charges
        String totalOtherCharges;

        switch ((String) dealerTrackData.get("buyerState")) {
            case "TX":
                totalOtherCharges = moneyFormatter.format(Double.parseDouble(dealerTrackData.get("Text56").toString().
                        replace(",", "")) + serviceContractDouble + regFeesDouble + documentFeeDouble);
                break;
            case "WA":
                totalOtherCharges = moneyFormatter.format(Double.parseDouble(dealerTrackData.get("Text56").toString().
                        replace(",", "")) + serviceContractDouble + regFeesDouble);
                break;
            default:
                totalOtherCharges = moneyFormatter.format(serviceContractDouble + regFeesDouble);
                break;
        }

        dealerTrackData.put("undefined_42", totalOtherCharges);

    }

    private static String tryAllyBox (JavascriptExecutor jse, int eltNum) throws WebDriverException {
        return (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                "document.body.childNodes[2].contentDocument.getElementById('appDetails')." +
                "contentDocument.getElementById('divHtml').childNodes[1].childNodes[1].childNodes[4]." +
                "childNodes[1].childNodes[3].childNodes[1].childNodes[" + eltNum + "].childNodes[1].childNodes[2]." +
                "childNodes[3].childNodes[1].childNodes[0].childNodes[3].firstChild.data");
    }

    public static void main(String[] args){

        try {
            pseudoMain(args);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void pseudoMain(String[] args) throws IOException {
        if (args.length < 3 || args.length > 5)
            throw new IllegalArgumentException("Usage: lastName bankName deliveryDate [firstName] [print]\n" +
                    "e.g. Hakim Ally 01/01/2016 OR Hakim Chase 1/1/2016 William print\n");

        if (args[args.length - 1].equals("print")) {
            print = true;
        }

        WebDriver driver = getNewFirefoxDriver();

        driver.get("https://www.dealertrack.com/default1.aspx?RefreshBy=SM");

        HashMap dealerTrackData = new HashMap();

        try {

            WebElement username = driver.findElement(By.name("username"));
            username.sendKeys("whakim");
            WebElement pwd = driver.findElement(By.name("password"));
            pwd.sendKeys("keepbeepin23");
            driver.findElement(By.name("login")).submit();

            JavascriptExecutor jse = (JavascriptExecutor) driver;

            jse.executeScript("var script = document.createElement('script');\n" +
                    "script.src = \"https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js\";\n" +
                    "document.getElementsByTagName('head')[0].appendChild(script);");

            jse.executeScript("document.getElementById('iFrm').contentWindow.document.body." +
                    "firstElementChild.contentDocument.getElementById('AppStatus').click()");

            int attempts = 0;
            while (attempts++ < 15) {
                Thread.sleep(500);
                try {
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                            "contentDocument.getElementById('txtSearchValue').value = '" + args[0] + "'");
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                            "contentDocument.getElementById('btnSearch').click();");
                    break;
                }
                catch (WebDriverException e) {
                    if (attempts >= 15)
                        e.printStackTrace();
                }
            }

            jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                    "contentDocument.getElementById('btnSearch').click();");

            attempts = 0;
            String firstLenderName = null;
            int boxNum = -1;

            while (attempts++ < 10) {
                Thread.sleep(750);
                try {
                    String boxName = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document." +
                            "body.childNodes[2].contentDocument.getElementById('applications_ctrl" + ++boxNum +
                            "_Tapplications_ctrl" + boxNum + "_TR').innerHTML");
                    int comma = boxName.indexOf(',');

                    if (comma == -1 || !(boxName.substring(0, comma).contains(args[0]))) {
                        boxNum--;
                        continue;
                    }

                    else if (args.length > 3 && !args[3].equals("print") && !boxName.substring(comma, boxName.length()).contains(args[3]))
                        continue;

                    long numApplications = (long) jse.executeScript("return document.getElementById('iFrm').contentWindow.document." +
                            "body.childNodes[2].contentDocument.getElementsByClassName('lender_decisions')[" + boxNum + "]." +
                            "childNodes[0].childElementCount");
                    String firstLenderResult = (String) jse.executeScript("return document.getElementById('iFrm')." +
                            "contentWindow.document.body.childNodes[2].contentDocument.getElementsByClassName" +
                            "('lender_decisions')[" + boxNum + "].childNodes[0].childNodes[0].className");

                    firstLenderName = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                            "document.body.childNodes[2].contentDocument.getElementsByClassName('lender_decisions')[" + boxNum + "]." +
                            "childNodes[0].childNodes[0].childNodes[0].innerHTML");

                    if (!firstLenderName.equals(args[1]) && numApplications == 1)
                        continue;

                    String secondLenderResult = null;

                    if (numApplications > 1) {
                        secondLenderResult = (String) jse.executeScript("return document.getElementById('iFrm')." +
                                "contentWindow.document.body.childNodes[2].contentDocument.getElementsByClassName" +
                                "('lender_decisions')[" + boxNum + "].childNodes[0].childNodes[1].className");
                    }

                    if (firstLenderResult.contains("declined") &&
                            ((numApplications > 1) ? secondLenderResult.contains("declined") : secondLenderResult == null))
                        continue;

                    break;

                }
                catch (WebDriverException e) {
                    boxNum--;
                    if (attempts >= 10)
                        e.printStackTrace();
                }
            }

            attempts = 0;
            String apr = null;

            if (args[1].equals("Chase")) {

                if (firstLenderName.equals("Chase"))
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                            "getElementsByClassName('lender_decisions')[" + boxNum + "].childNodes[0].childNodes[0].childNodes[0].click()");

                else
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                            "getElementsByClassName('lender_decisions')[" + boxNum + "].childNodes[0].childNodes[1].childNodes[0].click()");


                // APR
                while (attempts++ < 10) {
                    Thread.sleep(1500);
                    try {
                        apr = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document." +
                                "body.childNodes[2].contentDocument.getElementById('appDetails').contentDocument." +
                                "getElementById('divHtml').childNodes[1].childNodes[1].childNodes[4].childNodes[1]." +
                                "childNodes[5].firstElementChild.childNodes[0].childNodes[3].childNodes[3].childNodes[1]." +
                                "childNodes[10].childNodes[3].childNodes[0].nodeValue");
                        break;
                    }
                    catch (WebDriverException e) {
                        if (attempts >= 10)
                            e.printStackTrace();
                    }
                }

                dealerTrackData.put("Text201", "JPMorgan Chase Bk");
                dealerTrackData.put("fullBankName", "JPMorgan Chase Bk\nPO Box 901098\nFort Worth, TX 76101-2098");

            }

            else if (args[1].equals("Ally")) {

                if (firstLenderName.equals("Ally"))
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                            "getElementsByClassName('lender_decisions')[" + boxNum + "].childNodes[0].childNodes[0].childNodes[0].click()");

                else
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                            "getElementsByClassName('lender_decisions')[" + boxNum + "].childNodes[0].childNodes[1].childNodes[0].click()");

                // APR
                while (attempts++ < 10) {
                    Thread.sleep(1500);
                    try {
                        apr = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document." +
                                "body.childNodes[2].contentDocument.getElementById('appDetails').contentDocument." +
                                "getElementById('divHtml').childNodes[1].childNodes[1].childNodes[4].childNodes[1].childNodes[3]." +
                                "childNodes[1].childNodes[2].childNodes[1].childNodes[2].childNodes[3].childNodes[1]." +
                                "lastElementChild.childNodes[3].childNodes[0].data");
                        break;
                    }
                    catch (WebDriverException e) {
                        if (attempts >= 10)
                            e.printStackTrace();
                    }

                }

                // Lienholder Name
                String lienName = null;
                attempts = 0;

                for (int i = 6; lienName == null || !(lienName.contains(";") && lienName.contains("Ally")); i += 2, attempts++) {
                    try {
                        lienName = tryAllyBox(jse, i);
                    }
                    catch (WebDriverException e) {
                        if (attempts >= 10)
                            e.printStackTrace();
                        Thread.sleep(50);
                    }
                }

                dealerTrackData.put("Text201", lienName.substring(1, lienName.indexOf(";")));

                dealerTrackData.put("fullBankName", "Ally Bank\nP.O. Box 8128\nCockeysville, MD 21030");
            }

            else
                throw new IOException("Unsupported bank: " + args[1] + ". Choose Chase or Ally.");


            dealerTrackData.put("Text15", apr.substring(0, apr.length() - 2));

            Thread.sleep(1200);

            // Navigate to Application Info
            attempts = 0;
            while (attempts++ < 10) {
                Thread.sleep(50);
                try {
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                            "getElementById('appDetails').contentDocument.getElementById('divBtn').childNodes[4].childNodes[1].click()");
                    break;
                }
                catch (WebDriverException e) {
                    if (attempts >= 10)
                        e.printStackTrace();
                }
            }

            attempts = 0;
            while (attempts++ < 15) {
                Thread.sleep(500);
                try {
                    try {
                        jse.executeScript("document.getElementById('iFrm').contentWindow.document.body." +
                                "childNodes[2].contentDocument.getElementById('optAction_0').checked = true");
                        break;
                    }
                    catch (WebDriverException e) {
                        jse.executeScript("document.getElementById('iFrm').contentWindow.document.body." +
                                "childNodes[2].contentDocument.getElementById('optAction_copy_0').checked = true");
                        break;
                    }
                } catch (WebDriverException e) {
                    if (attempts >= 15) {
                        e.printStackTrace();
                    }
                }
            }

            attempts = 0;
            while (attempts++ < 10) {
                Thread.sleep(250);
                try {
                    jse.executeScript("document.getElementById('iFrm').contentWindow.document.body.childNodes[2]." +
                            "contentDocument.getElementById('btncontinue').click()");
                    break;
                }
                catch (WebDriverException e){
                    if (attempts >= 10) {
                        e.printStackTrace();
                    }
                }
            }

            // Sale ID
            attempts = 0;
            String sid = null;

            while (attempts++ < 20) {
                Thread.sleep(750);
                try {
                    sid = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                            "childNodes[2].contentDocument.getElementById('app_stock_num').value");
                    break;
                }
                catch (WebDriverException e) {
                    if (attempts >= 20) {
                        e.printStackTrace();
                    }
                }
            }

            dealerTrackData.put("Text3", sid);

            // Figure out what state the buyer is in
            dealerTrackData.put("buyerState", (String) jse.executeScript("return document.getElementById('iFrm')." +
                    "contentWindow.document.body.childNodes[2].contentDocument.getElementById('app_state').value"));

            // Buyer Name and Address
            String firstName = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_first_name').value");

            String lastName = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_last_name').value");

            // Store applicant's name for PDF naming
            String fullName = firstName + " " + lastName;
            dealerTrackData.put("fullName", fullName);

            String address0 = (String) jse.executeScript("return ((document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_street_num').value).concat(' '))." +
                    "concat(document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                    "getElementById('app_street_name').value)");

            String aptNum = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_apt_num').value");

            if (!aptNum.equals(""))
                address0 = address0 + " " + aptNum;

            String city = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_city').value");

            String state = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_state').value");

            String zipCode = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_zip_code').value");

            dealerTrackData.put("Text5", fullName + "\n" + address0 + "\n" + city + ", " + state + " " + zipCode);

            // Fill Name/Address Lines in Ally Credit App + Confirmation of Insurance, if applicable

            if (args[1].equals("Ally") || dealerTrackData.get("buyerState").equals("TX")) {
                dealerTrackData.put("Last Name (or trade name of business)", lastName);
                dealerTrackData.put("First", firstName);
                dealerTrackData.put("Present Address", address0);
                dealerTrackData.put("Zip Code", zipCode);
                dealerTrackData.put("City", city);
                dealerTrackData.put("State", state);
            }

            dealerTrackData.put("addressLine", address0 + ", " + city + ", " + state + " " + zipCode);


            boolean cosigner = false;

            // Co-Signer Name and Address (if applicable)
            try {
                String coAppFirstName = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_first_name').value");

                String coAppLastName = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_last_name').value");

                String coAppFullName = coAppFirstName + ' ' + coAppLastName;

                // Store Co-App Full Name (for TX Contract)
                dealerTrackData.put("coAppFullName", coAppFullName);

                String coAppAddress0 = (String) jse.executeScript("return ((document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_street_num').value).concat(' '))." +
                        "concat(document.getElementById('iFrm').contentWindow.document.body.childNodes[2].contentDocument." +
                        "getElementById('app_co_street_name').value)");

                String coAppAptNum = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_apt_num').value");

                if (!coAppAptNum.equals(""))
                    coAppAddress0 = coAppAddress0 + " " + coAppAptNum;

                String coAppCity = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_city').value");

                String coAppState = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_state').value");

                String coAppZipCode = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                        "document.body.childNodes[2].contentDocument.getElementById('app_co_zip_code').value");

                dealerTrackData.put("Text6", coAppFullName + "\n" + coAppAddress0 + "\n" + coAppCity + ", " + coAppState
                    + " " + coAppZipCode);

                if (args[1].equals("Ally") || dealerTrackData.get("buyerState").equals("TX")) {
                    dealerTrackData.put("First2", coAppFirstName);
                    dealerTrackData.put("Last Name (or trade nameof business)2", coAppLastName);
                    dealerTrackData.put("Present Address2", coAppAddress0);
                    dealerTrackData.put("Zip Code2", coAppZipCode);
                    dealerTrackData.put("City2", coAppCity);
                    dealerTrackData.put("State2", coAppState);
                }

                cosigner = true;

            }
            catch (WebDriverException e) {
                dealerTrackData.put("cosignerNullField", "N/A");
            }

            // Car Year
            String carYear = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementById('app_auto_yearList').value");
            dealerTrackData.put("Text9", carYear);

            // Car Make and Model
            String carMake = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_auto_makeList').value");

            String carModel = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_auto_modelList').value");

            dealerTrackData.put("Text10", carMake + ' ' + carModel);

            dealerTrackData.put("Make", carMake);
            dealerTrackData.put("Model", carModel);

            // Style/Trim
            String carStyleTrim = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow." +
                    "document.body.childNodes[2].contentDocument.getElementById('app_auto_trimList').value");
            dealerTrackData.put("StylefTrim", carStyleTrim);

            // VIN
            String vin = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementById('app_vin_num').value");
            dealerTrackData.put("Text12", vin);

            // Amount Financed
            if (args[1].equals("Chase"))
                recalculateChaseStructure(jse);

            String amountFinanced = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_est_amt_financed')[0].value");
            double amountFinancedDouble = Double.parseDouble(amountFinanced);
            String amountFinancedFormatted = moneyFormatter.format(amountFinancedDouble);
            dealerTrackData.put("Text17", amountFinancedFormatted);

            // Term
            String loanTerm = (jse.executeScript("return (document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_terms')[0].value - 1)").toString());
            dealerTrackData.put("Number Payments 4", dealerTrackData.get("buyerState").equals("CA") ? loanTerm :
                    Integer.toString(Integer.parseInt(loanTerm) + 1));

            // Meaty Loan Calculations
            double aprDouble = (Double.parseDouble((String) dealerTrackData.get("Text15")) / (Double) 100.);
            int loanTermInt = Integer.parseInt(loanTerm) + 1;

            double monthlyPayment = Double.parseDouble(moneyFormatter.format(amountFinancedDouble *
                    (aprDouble / 12.) / (1. - Math.pow(1. + aprDouble / 12., -loanTermInt))));
            String roundMonthlyPayment = moneyFormatter.format(monthlyPayment);


            double totalOfPayments = monthlyPayment * loanTermInt;
            String roundTotalOfPayments = moneyFormatter.format(totalOfPayments);

            // Total of Payments
            dealerTrackData.put("Text18", roundTotalOfPayments);

            // Finance Charge (take care of down payment later)
            dealerTrackData.put("Text16", moneyFormatter.format(totalOfPayments - amountFinancedDouble));

            // Monthly Payments
            dealerTrackData.put("Text26", roundMonthlyPayment);
            dealerTrackData.put("Text31", roundMonthlyPayment);

            // First and Last Payment Date
            DateTimeFormatter ddFormat = DateTimeFormatter.ofPattern("M/d/yyyy");
            LocalDate dd = LocalDate.parse(args[2], ddFormat);
            int plusDays = (args[1].equals("Chase")) ? 45 : 30;

            LocalDate firstPayment = dd.plusDays(plusDays);
            LocalDate lastPayment = firstPayment.plusMonths(loanTermInt - 1);

            String firstPaymentFormatted = firstPayment.format(ddFormat);
            String lastPaymentFormatted = lastPayment.format(ddFormat);

            dealerTrackData.put("Text27", firstPaymentFormatted);
            dealerTrackData.put("Text32", lastPaymentFormatted);

            // Car Base Price
            String carBasePrice = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_price')[0].value");
            double carBasePriceDouble = Double.parseDouble(carBasePrice);
            String carBasePriceFormatted = moneyFormatter.format(carBasePriceDouble);

            dealerTrackData.put("Text33", carBasePriceFormatted);
            dealerTrackData.put("Text34", carBasePriceFormatted);

            // Sales Tax
            String carTaxes = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_sales_tax')[0].value");
            double carTaxesDouble = Double.parseDouble(carTaxes);
            String carTaxesFormatted = moneyFormatter.format(carTaxesDouble);

            dealerTrackData.put("Text49", carTaxesFormatted);

            // Extended Peace of Mind
            String serviceContract = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_warranty')[0].value");
            Double serviceContractDouble = 0.;

            if (!serviceContract.equals("")) {
                serviceContractDouble = Double.parseDouble(serviceContract);

                dealerTrackData.put("Text51", moneyFormatter.format(serviceContractDouble));
                // For California Contract
                dealerTrackData.put("Text94", "Beepi Peace of Mind");
                dealerTrackData.put("7/13_1LCompany", "Beepi Peace of Mind");
                // For Arizona or Texas Contract
                dealerTrackData.put("Beepi", "Beepi");
                dealerTrackData.put("na5", "Extended Peace of Mind");

            }

            // Documentary Service Fees
            String documentFee = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_other_finance_fees')[0].value");
            Double documentFeeDouble = 0.;

            if (!documentFee.equals("")) {
                documentFeeDouble = Double.parseDouble(documentFee);
                dealerTrackData.put("Text42", moneyFormatter.format(documentFeeDouble));
            }

            // Registration Fees
            String regFees = (String) jse.executeScript("return document.getElementById('iFrm').contentWindow.document.body." +
                    "childNodes[2].contentDocument.getElementsByName('app_ttl')[0].value");
            double regFeesDouble = Double.parseDouble(regFees);
            String regFeesFormatted = moneyFormatter.format(regFeesDouble);

            dealerTrackData.put("Text62", regFeesFormatted);
            dealerTrackData.put("Text65", regFeesFormatted);

            // Calculate Contract Based on State
            switch ((String) dealerTrackData.get("buyerState")) {
                case "CA":
                    californiaContract(args, jse, dealerTrackData, carBasePriceDouble, carTaxesDouble,
                            serviceContractDouble, regFeesDouble, documentFeeDouble, totalOfPayments);
                    break;
                case "AZ": case "TX": case "WA":
                    arizonaTexasWashingtonContract(args, jse, dealerTrackData, carBasePriceDouble, carTaxesDouble,
                            serviceContractDouble, regFeesDouble, documentFeeDouble, totalOfPayments);
                    break;
            }

            // Amount Financed
            dealerTrackData.put("Text77", amountFinancedFormatted);

            // Delivery Date (Signatures)
            String ddFormatted = dd.format(ddFormat);

            dealerTrackData.put("Text159", ddFormatted);
            dealerTrackData.put("Text165", ddFormatted);

            if (cosigner)
                dealerTrackData.put("Text160", ddFormatted);

            // Populate Credit Applications
            if (args[1].equals("Ally"))
                getAllyCreditInfo(jse, dealerTrackData, cosigner);
            else
                getChaseCreditInfo(jse, dealerTrackData, driver);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        driver.quit();

        String contractDir = "DO NOT EDIT - AutoFill Contracts/";

        try {

            if (args[1].equals("Ally")) {
                outputDir = makeOutputDir(dealerTrackData);
                fillPdf(dealerTrackData, contractDir + "Blank Ally Credit Application.pdf", outputDir, "Ally Credit Application");
                fillPdf(dealerTrackData, contractDir + "Blank Ally Odometer Disclosure.pdf", outputDir, "Ally Odometer Disclosure");
                fillPdf(dealerTrackData, contractDir + "Blank Ally Insurance Form.pdf", outputDir, "Ally Insurance Form");
                fillPdf(dealerTrackData, contractDir + "Ally Title Guarantee.pdf", outputDir, "Guarantee of Title");
            }

            else {
                fillPdf(dealerTrackData, contractDir + "Blank Agreement to Furnish Insurance.pdf", outputDir, "Chase Insurance Form");
                fillPdf(dealerTrackData, contractDir + "Chase Title Guarantee.pdf", outputDir, "Guarantee of Title");
            }

            switch((String) dealerTrackData.get("buyerState")) {
                case "CA":
                    fillPdf(dealerTrackData, contractDir + "Blank Purchase Contract-N-A'd out.pdf", outputDir, "Purchase Contract");
                    break;
                case "AZ":
                    fillPdf(dealerTrackData, contractDir + "Blank AZ Purchase Contract-N-A'd out.pdf", outputDir, "Purchase Contract");
                    break;
                case "TX":
                    fillPdf(dealerTrackData, contractDir + "Blank TX Purchase Contract-N-A'd out.pdf", outputDir, "Purchase Contract");
                    break;
                case "WA":
                    fillPdf(dealerTrackData, contractDir + "Blank WA Purchase Contract-N-A'd out.pdf", outputDir, "Purchase Contract");
                    break;
            }



        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}