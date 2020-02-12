/*
Job Management & Tracking System (JMTS) 
Copyright (C) 2017  D P Bennett & Associates Limited

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

Email: info@dpbennett.com.jm
 */
package jm.com.dpbennett.jmts.manager;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedProperty;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import jm.com.dpbennett.business.entity.fm.Classification;
import jm.com.dpbennett.business.entity.cm.Client;
import jm.com.dpbennett.business.entity.hrm.Contact;
import jm.com.dpbennett.business.entity.rm.DatePeriod;
import jm.com.dpbennett.business.entity.hrm.Department;
import jm.com.dpbennett.business.entity.hrm.Employee;
import jm.com.dpbennett.business.entity.fm.JobCategory;
import jm.com.dpbennett.business.entity.fm.JobCostingAndPayment;
import jm.com.dpbennett.business.entity.hrm.User;
import jm.com.dpbennett.business.entity.fm.JobSubCategory;
import jm.com.dpbennett.business.entity.sm.Preference;
import jm.com.dpbennett.business.entity.fm.Sector;
import jm.com.dpbennett.business.entity.jmts.ServiceContract;
import jm.com.dpbennett.business.entity.jmts.ServiceRequest;
import jm.com.dpbennett.business.entity.sm.SystemOption;
import jm.com.dpbennett.business.entity.fm.AccPacCustomer;
import jm.com.dpbennett.business.entity.hrm.Address;
import jm.com.dpbennett.business.entity.hrm.BusinessOffice;
import jm.com.dpbennett.business.entity.jmts.Job;
import jm.com.dpbennett.business.entity.util.BusinessEntityUtils;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.StreamedContent;
import jm.com.dpbennett.business.entity.gm.BusinessEntityManagement;
import jm.com.dpbennett.business.entity.sm.Alert;
import jm.com.dpbennett.business.entity.util.ReturnMessage;
import jm.com.dpbennett.cm.manager.ClientManager;
import jm.com.dpbennett.fm.manager.FinanceManager;
import jm.com.dpbennett.hrm.manager.HumanResourceManager;
import jm.com.dpbennett.jmts.JMTSApplication;
import jm.com.dpbennett.lo.manager.LegalDocumentManager;
import jm.com.dpbennett.pm.manager.PurchasingManager;
import jm.com.dpbennett.rm.manager.ReportManager;
import jm.com.dpbennett.sm.Authentication.AuthenticationListener;
import jm.com.dpbennett.sm.manager.SystemManager;
import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.UnselectEvent;
import jm.com.dpbennett.sm.util.BeanUtils;
import jm.com.dpbennett.sm.util.Dashboard;
import jm.com.dpbennett.sm.util.DateUtils;
import jm.com.dpbennett.sm.util.JobDataModel;
import jm.com.dpbennett.sm.util.MainTabView;
import jm.com.dpbennett.sm.util.PrimeFacesUtils;
import jm.com.dpbennett.sm.util.ReportUtils;
import jm.com.dpbennett.sm.util.Utils;

/**
 *
 * @author Desmond Bennett
 */
public class JobManager implements
        Serializable, BusinessEntityManagement, AuthenticationListener {

    private JMTSApplication application;
    @PersistenceUnit(unitName = "JMTSPU")
    private EntityManagerFactory EMF1;
    @PersistenceUnit(unitName = "AccPacPU")
    private EntityManagerFactory EMF2;
    private Job currentJob;
    private Job selectedJob;
    @ManagedProperty(value = "Jobs")
    private Integer longProcessProgress;
    private Boolean useAccPacCustomerList;
    private Boolean showJobEntry;
    private List<Job> jobSearchResultList;
    private DatePeriod dateSearchPeriod;
    private String searchType;
    private String searchText;
    private Job[] selectedJobs;
    private AccPacCustomer accPacCustomer;

    /**
     * Creates a new instance of JobManager
     */
    public JobManager() {
        init();
    }

    public String getApplicationHeader() {
        return "Job Management & Tracking System";
    }

    public String getRenderDateSearchFields() {
        switch (searchType) {
            case "Suppliers":
                return "false";
            default:
                return "true";
        }
    }

    /**
     * Gets the ApplicationScoped object that is associated with this webapp.
     *
     * @return
     */
    public JMTSApplication getApplication() {
        if (application == null) {
            application = BeanUtils.findBean("App");
        }
        return application;
    }

    /**
     * Completes a list of active job subcategories based on the query string
     * provided.
     *
     * @param query
     * @return
     */
    public List<JobSubCategory> completeActiveJobSubCategories(String query) {
        EntityManager em = getEntityManager1();

        List<JobSubCategory> subCategories = JobSubCategory.findAllActiveJobSubCategories(em);

        return subCategories;
    }

    /**
     * Completes a list of active job categories based on the query string
     * provided.
     *
     * @param query
     * @return
     */
    public List<JobCategory> completeActiveJobCategories(String query) {
        EntityManager em = getEntityManager1();

        List<JobCategory> categories = JobCategory.findAllActiveJobCategories(em);

        return categories;
    }

    /**
     * NB: query parameter currently not used to filter sectors.
     *
     * @param query
     * @return
     */
    public List<Sector> completeActiveSectors(String query) {
        EntityManager em = getEntityManager1();

        List<Sector> sectors = Sector.findAllActiveSectors(em);

        return sectors;
    }

    public List<String> getJobTableViews() {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<String> preferenceValues = Preference.findAllPreferenceValues(em, "");

            return preferenceValues;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<Classification> completeJobClassification(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<Classification> classifications = Classification.findActiveClassificationsByNameAndCategory(em, query, "Job");

            return classifications;
        } catch (Exception e) {

            System.out.println(e);
            return new ArrayList<>();
        }
    }

    /**
     * Finds an Accpac customer by name and updates the Accpac customer field.
     *
     * @param event
     */
    public void updateAccPacCustomer(SelectEvent event) {
        EntityManager em = getEntityManager2();

        accPacCustomer = AccPacCustomer.findByName(em, accPacCustomer.getCustomerName().trim());
        if (accPacCustomer != null) {
            if (accPacCustomer.getIdCust() != null) {
                accPacCustomer.setIsDirty(true);
            }
        }
    }

    /**
     * Gets the Accpac customer field.
     *
     * @return
     */
    public AccPacCustomer getAccPacCustomer() {
        if (accPacCustomer == null) {
            accPacCustomer = new AccPacCustomer();
        }
        return accPacCustomer;
    }

    /**
     * Sets the Accpac customer field.
     *
     * @param accPacCustomer
     */
    public void setAccPacCustomer(AccPacCustomer accPacCustomer) {
        this.accPacCustomer = accPacCustomer;
    }

    public void onJobCostingSelect(SelectEvent event) {
    }

    public void onJobCostingUnSelect(UnselectEvent event) {
    }

    /**
     * Handles the editing of cells in the Job Costing table.
     *
     * @param event
     */
    public void onJobCostingCellEdit(CellEditEvent event) {

        // Set edited by
        getJobSearchResultList().get(event.getRowIndex()).
                getClient().setEditedBy(getUser().getEmployee());

        // Set date edited
        getJobSearchResultList().get(event.getRowIndex()).
                getClient().setDateEdited(new Date());

        // Set the Accounting ID
        getJobSearchResultList().get(event.getRowIndex()).
                getClient().setAccountingId(
                        getJobSearchResultList().get(event.getRowIndex()).
                                getClient().getFinancialAccount().getIdCust());

        // Set credit limit
        getJobSearchResultList().get(event.getRowIndex()).
                getClient().setCreditLimit(
                        getJobSearchResultList().get(event.getRowIndex()).
                                getClient().getFinancialAccount().getCreditLimit().doubleValue());

        // Save
        getJobSearchResultList().get(event.getRowIndex()).
                getClient().save(getEntityManager1());

    }

    /**
     * Handles the initialization of the JobManager session bean.
     *
     */
    private void init() {
        reset();

        getSystemManager().addSingleAuthenticationListener(this);
    }

    /**
     * Get LegalDocumentManager SessionScoped bean.
     *
     * @return
     */
    public LegalDocumentManager getLegalDocumentManager() {

        return BeanUtils.findBean("legalDocumentManager");
    }

    /**
     * Get JobContractManager SessionScoped bean.
     *
     * @return
     */
    public JobContractManager getJobContractManager() {

        return BeanUtils.findBean("jobContractManager");
    }

    /**
     * Get JobSampleManager SessionScoped bean.
     *
     * @return
     */
    public JobSampleManager getJobSampleManager() {

        return BeanUtils.findBean("jobSampleManager");
    }

    /**
     * Get JobFinanceManager SessionScoped bean.
     *
     * @return
     */
    public JobFinanceManager getJobFinanceManager() {

        return BeanUtils.findBean("jobFinanceManager");
    }

    public PurchasingManager getPurchasingManager() {
        return BeanUtils.findBean("purchasingManager");
    }

    /**
     * Get FinanceManager SessionScoped bean.
     *
     * @return
     */
    public FinanceManager getFinanceManager() {

        return BeanUtils.findBean("financeManager");
    }

    /**
     * Gets the ReportManager SessionScoped bean.
     *
     * @return
     */
    public ReportManager getReportManager() {
        return BeanUtils.findBean("reportManager");
    }

    /**
     * Gets the ClientManager SessionScoped bean.
     *
     * @return
     */
    public ClientManager getClientManager() {

        return BeanUtils.findBean("clientManager");
    }

    public HumanResourceManager getHumanResourceManager() {
        return BeanUtils.findBean("humanResourceManager");
    }

    /**
     * Gets the date search period for jobs.
     *
     * @return
     */
    public DatePeriod getDateSearchPeriod() {
        return dateSearchPeriod;
    }

    public void setDateSearchPeriod(DatePeriod dateSearchPeriod) {
        this.dateSearchPeriod = dateSearchPeriod;
    }

    public void updateDateSearchField() {
        //doSearch();
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public ArrayList getSearchTypes() {
        ArrayList searchTypes = new ArrayList();

        searchTypes.add(new SelectItem("General", "General"));
        searchTypes.add(new SelectItem("My jobs", "My jobs"));
        searchTypes.add(new SelectItem("My department's jobs", "My department's jobs"));
        searchTypes.add(new SelectItem("Parent jobs only", "Parent jobs only"));
        searchTypes.add(new SelectItem("Unapproved job costings", "Unapproved job costings"));
        searchTypes.add(new SelectItem("Appr'd & uninv'd jobs", "Appr'd & uninv'd jobs"));
        searchTypes.add(new SelectItem("Incomplete jobs", "Incomplete jobs"));
        //searchTypes.add(new SelectItem("Purchase requisitions", "Purchase requisitions"));
        //searchTypes.add(new SelectItem("Suppliers", "Suppliers"));

        return searchTypes;
    }

    public ArrayList getDateSearchFields() {
        ArrayList dateSearchFields = new ArrayList();

        switch (searchType) {
            case "Suppliers":
                //  dateSearchFields.add(new SelectItem("dateEntered", "Date entered"));
                //  dateSearchFields.add(new SelectItem("dateEdited", "Date edited"));
                break;
            case "Purchase requisitions":
                dateSearchFields.add(new SelectItem("requisitionDate", "Requisition date"));
                dateSearchFields.add(new SelectItem("dateOfCompletion", "Date completed"));
                dateSearchFields.add(new SelectItem("dateEdited", "Date edited"));
                dateSearchFields.add(new SelectItem("expectedDateOfCompletion", "Exp'ted date of completion"));
                dateSearchFields.add(new SelectItem("dateRequired", "Date required"));
                dateSearchFields.add(new SelectItem("purchaseOrderDate", "Purchase order date"));
                dateSearchFields.add(new SelectItem("teamLeaderApprovalDate", "Team Leader approval date"));
                dateSearchFields.add(new SelectItem("divisionalManagerApprovalDate", "Divisional Manager approval date"));
                dateSearchFields.add(new SelectItem("divisionalDirectorApprovalDate", "Divisional Director approval date"));
                dateSearchFields.add(new SelectItem("financeManagerApprovalDate", "Finance Manager approval date"));
                dateSearchFields.add(new SelectItem("executiveDirectorApprovalDate", "Executive Director approval date"));
                break;
            default:
                return DateUtils.getDateSearchFields();
        }

        return dateSearchFields;
    }

    public ArrayList getAuthorizedSearchTypes() {

        // Filter list based on user's authorization
        EntityManager em = getEntityManager1();

        if (getUser(em).getPrivilege().getCanEditJob()
                || getUser(em).getPrivilege().getCanEnterJob()
                || getUser(em).getPrivilege().getCanEditInvoicingAndPayment()
                || getUser(em).getEmployee().getDepartment().getPrivilege().getCanEditInvoicingAndPayment()
                || getUser(em).getEmployee().getDepartment().getPrivilege().getCanEditJob()
                || getUser(em).getEmployee().getDepartment().getPrivilege().getCanEnterJob()) {
            return getSearchTypes();
        } else {

            ArrayList newList = new ArrayList();
            for (Object obj : getSearchTypes()) {
                SelectItem item = (SelectItem) obj;
                if (!item.getLabel().equals("General")
                        && !item.getLabel().equals("Unapproved job costings")
                        && !item.getLabel().equals("Incomplete jobs")) {
                    newList.add(item);
                }
            }

            return newList;
        }

    }

    public void clientDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentJob().setClient(getClientManager().getSelectedClient());
        }
    }

    public void jobDialogReturn() {
        if (currentJob.getIsDirty()) {
            PrimeFacesUtils.addMessage("Job was NOT saved", "The recently edited job was not saved", FacesMessage.SEVERITY_WARN);
            PrimeFaces.current().ajax().update("headerForm:growl3");
            currentJob.setIsDirty(false);
        }

    }

    private void initManagers() {
        try {

            getClientManager();
            getJobContractManager();
            getJobFinanceManager();
            getFinanceManager();
            getPurchasingManager();
            getJobSampleManager();
            getReportManager();
            getHumanResourceManager();
            getLegalDocumentManager();

        } catch (Exception e) {
            System.out.println("An error occured while resetting managers: " + e);
        }
    }

    public void reset() {
        showJobEntry = false;
        longProcessProgress = 0;
        useAccPacCustomerList = false;
        jobSearchResultList = new ArrayList<>();
        searchType = "";
        dateSearchPeriod = new DatePeriod("This month", "month",
                "dateAndTimeEntered", null, null, null, false, false, false);
        dateSearchPeriod.initDatePeriod();

        initManagers();
    }

    public Boolean getCanApplyTax() {
        return JobCostingAndPayment.getCanApplyTax(getCurrentJob());
    }

    public MainTabView getMainTabView() {
        return getSystemManager().getMainTabView();
    }

    public EntityManager getEntityManager1() {
        return EMF1.createEntityManager();
    }

    public User getUser() {
        return getSystemManager().getAuthentication().getUser();
    }

    /**
     * Get user as currently stored in the database
     *
     * @param em
     * @return
     */
    public User getUser(EntityManager em) {
        return getSystemManager().getAuthentication().getUser(em);
    }

    public Dashboard getDashboard() {
        return getSystemManager().getDashboard();
    }

    public void prepareToCloseJobDetail() {
        PrimeFacesUtils.closeDialog(null);
    }

    /**
     * Get selected jobs which are usually displayed in a table.
     *
     * @return
     */
    public Job[] getSelectedJobs() {
        if (selectedJobs == null) {
            selectedJobs = new Job[]{};
        }
        return selectedJobs;
    }

    public void setSelectedJobs(Job[] selectedJobs) {
        this.selectedJobs = selectedJobs;
    }

    public void openJobBrowser() {
        // Set "Job View" based on search type
        if (getSearchType().equals("Unapproved job costings")) {
            getUser().setJobTableViewPreference("Job Costings");
        }

        getSystemManager().getMainTabView().openTab("Job Browser");
    }

    public void openSystemAdministrationTab() {
        getSystemManager().getMainTabView().openTab("System Administration");
    }

    public void openFinancialAdministrationTab() {
        getSystemManager().getMainTabView().openTab("Financial Administration");
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public Boolean getShowJobEntry() {
        return showJobEntry;
    }

    public void setShowJobEntry(Boolean showJobEntry) {
        this.showJobEntry = showJobEntry;
    }

    private Boolean isCurrentJobJobAssignedToUser() {
        if (getUser() != null) {
            return currentJob.getAssignedTo().getId().longValue() == getUser().getEmployee().getId().longValue();
        } else {
            return false;
        }
    }

    private Boolean isJobAssignedToUserDepartment() {

        if (getUser() != null) {
            if (currentJob.getDepartment().getId().longValue() == getUser().getEmployee().getDepartment().getId().longValue()) {
                return true;
            } else {
                return currentJob.getSubContractedDepartment().getId().longValue()
                        == getUser().getEmployee().getDepartment().getId().longValue();
            }
        } else {
            return false;
        }
    }

    public Boolean getCanEnterJob() {
        if (getUser() != null) {
            return getUser().getPrivilege().getCanEnterJob();
        } else {
            return false;
        }
    }

    /**
     * Can edit job only if the job is assigned to your department or if you
     * have job entry privilege
     *
     * @return
     */
    public Boolean getCanEditDepartmentalJob() {
        if (getCanEnterJob()) {
            return true;
        }

        if (getUser() != null) {
            return getUser().getPrivilege().getCanEditDepartmentJob() && isJobAssignedToUserDepartment();
        } else {
            return false;
        }
    }

    public Boolean getCanEditOwnJob() {
        if (getCanEnterJob()) {
            return true;
        }

        if (getUser() != null) {
            return getUser().getPrivilege().getCanEditOwnJob();
        } else {
            return false;
        }
    }

    public Integer getLongProcessProgress() {
        if (longProcessProgress == null) {
            longProcessProgress = 0;
        } else {
            if (longProcessProgress < 10) {
                // this is to ensure that this method does not make the progress
                // complete as this is done elsewhere.
                longProcessProgress = longProcessProgress + 1;
            }
        }

        return longProcessProgress;
    }

//    public void onLongProcessComplete() {
//        longProcessProgress = 0;
//    }
//    public void setLongProcessProgress(Integer longProcessProgress) {
//        this.longProcessProgress = longProcessProgress;
//    }
    /**
     * For future implementation if necessary
     *
     * @param query
     * @return
     */
    public List<String> completeSearchText(String query) {
        List<String> suggestions = new ArrayList<>();

        return suggestions;
    }

    public void createNewJob() {

        EntityManager em = getEntityManager1();

        if (checkUserJobEntryPrivilege()) {
            createJob(em, false);
            getJobFinanceManager().setEnableOnlyPaymentEditing(false);
            PrimeFacesUtils.openDialog(null, "jobDialog", true, true, true, 600, 975);
            openJobBrowser();
        } else {
            PrimeFacesUtils.addMessage("Job NOT Created",
                    "You do not have the prvilege to create jobs. Please contact your System Administrator",
                    FacesMessage.SEVERITY_ERROR);
        }

    }

    public StreamedContent getServiceContractFile() {
        StreamedContent serviceContractStreamContent = null;

        try {

            serviceContractStreamContent = getJobContractManager().getServiceContractStreamContent();

//            setLongProcessProgress(100);
        } catch (Exception e) {
            System.out.println(e);
//            setLongProcessProgress(0);
        }

        return serviceContractStreamContent;
    }

    public Boolean getCurrentJobIsValid() {
        return getCurrentJob().getId() != null && !getCurrentJob().getIsDirty();
    }

    public List<Preference> getJobTableViewPreferences() {
        EntityManager em = getEntityManager1();

        List<Preference> prefs = Preference.findAllPreferencesByName(em, "jobTableView");

        return prefs;
    }

    /**
     * Conditionally disable department entry. Currently not used.
     *
     * @return
     */
    public Boolean getDisableDepartmentEntry() {

        // allow department entry only if business office is null
        if (currentJob != null) {
            if (currentJob.getBusinessOffice() != null) {
                return !currentJob.getBusinessOffice().getCode().trim().equals("");
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void setJobCompletionDate(Date date) {
        currentJob.getJobStatusAndTracking().setDateOfCompletion(date);
    }

    public Date getJobCompletionDate() {
        if (currentJob != null) {
            return currentJob.getJobStatusAndTracking().getDateOfCompletion();
        } else {
            return null;
        }
    }

    // NB: This and other code that get date is no longer necessary. Clean up!
    public Date getExpectedDateOfCompletion() {
        if (currentJob != null) {
            return currentJob.getJobStatusAndTracking().getExpectedDateOfCompletion();
        } else {
            return null;
        }
    }

    public void setExpectedDateOfCompletion(Date date) {
        currentJob.getJobStatusAndTracking().setExpectedDateOfCompletion(date);
    }

    public Date getDateDocumentCollected() {
        if (currentJob != null) {
            if (currentJob.getJobStatusAndTracking().getDateDocumentCollected() != null) {
                return currentJob.getJobStatusAndTracking().getDateDocumentCollected();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void setDateDocumentCollected(Date date) {
        currentJob.getJobStatusAndTracking().setDateDocumentCollected(date);
    }

    /**
     *
     * @return
     */
    public Boolean getCompleted() {
        if (currentJob != null) {
            return currentJob.getJobStatusAndTracking().getCompleted();
        } else {
            return false;
        }
    }

    public void setCompleted(Boolean b) {
        currentJob.getJobStatusAndTracking().setCompleted(b);
    }

    public Boolean getJobSaved() {
        return getCurrentJob().getId() != null;
    }

    public Boolean getSamplesCollected() {
        if (currentJob != null) {
            return currentJob.getJobStatusAndTracking().getSamplesCollected();
        } else {
            return false;
        }
    }

    public void setSamplesCollected(Boolean b) {
        currentJob.getJobStatusAndTracking().setSamplesCollected(b);
    }

    // documents collected by
    public Boolean getDocumentCollected() {
        if (currentJob != null) {
            return currentJob.getJobStatusAndTracking().getDocumentCollected();
        } else {
            return false;
        }
    }

    public void setDocumentCollected(Boolean b) {
        currentJob.getJobStatusAndTracking().setDocumentCollected(b);
    }

    public EntityManager getEntityManager2() {
        return EMF2.createEntityManager();
    }

    public void updateJobCategory() {
        setIsDirty(true);
    }

    public void updateJobSubCategory() {
        setIsDirty(true);
    }

    public void updateJob(AjaxBehaviorEvent event) {
        setIsDirty(true);
    }
    
    public void updateStartDate(AjaxBehaviorEvent event) {
        if ((getCurrentJob().getJobStatusAndTracking().getStartDate() != null)
                && getCurrentJob().getJobStatusAndTracking().getWorkProgress().equals("Not started")) {
           getCurrentJob().getJobStatusAndTracking().setWorkProgress("Ongoing");   
        }
        
        setIsDirty(true);
    }

    public void updateJobView(AjaxBehaviorEvent event) {
        getUser().save(getEntityManager1());
    }

    public void updateJobClassification() {

        // Setup default tax
        if (currentJob.getClassification().getDefaultTax().getId() != null) {
            currentJob.getJobCostingAndPayment().setTax(currentJob.getClassification().getDefaultTax());
        }

        // Get the clasification saved for use in setting taxes
        // Update all costs that depend on tax
        if (currentJob.getId() != null) {
            getJobFinanceManager().updateAllTaxes(null);
        }
    }

    public void updateTestsAndCalibration() {
        currentJob.setNoOfTestsOrCalibrations(currentJob.getNoOfTests() + currentJob.getNoOfCalibrations());

        setIsDirty(true);
    }

    public void update() {
        setIsDirty(true);
    }

    public void updateDocumentsCollectedBy() {
        setIsDirty(true);
        if (!currentJob.getJobStatusAndTracking().getDocumentCollected()) {
            currentJob.getJobStatusAndTracking().setDocumentCollectedBy("");
            setDateDocumentCollected(null);
        } else {
            setDateDocumentCollected(new Date());
        }
    }

    public void updateJobCompleted() {
        if (getCompleted()) {
            currentJob.getJobStatusAndTracking().setWorkProgress("Completed");
            setJobCompletionDate(new Date());
        } else {
            currentJob.getJobStatusAndTracking().setWorkProgress("Not started");
            setJobCompletionDate(null);
        }
        setIsDirty(true);
    }

    public void updateSamplesCollectedBy() {
        setIsDirty(true);
        if (!currentJob.getJobStatusAndTracking().getSamplesCollected()) {
            currentJob.getJobStatusAndTracking().setSamplesCollectedBy("");
            currentJob.getJobStatusAndTracking().setDateSamplesCollected(null);
        } else {
            currentJob.getJobStatusAndTracking().setDateSamplesCollected(new Date());
        }
    }

    public void updateJobReportNumber() {
        setIsDirty(true);
    }

    public void updateAutoGenerateJobNumber() {

        if (currentJob.getAutoGenerateJobNumber()) {
            currentJob.setJobNumber(getCurrentJobNumber());
        }
        setIsDirty(true);

    }

    public void updateNewClient() {
        setIsDirty(true);
    }

    public void updateSamplesCollected() {
        setIsDirty(true);
    }

    public Boolean checkWorkProgressReadinessToBeChanged() {
        EntityManager em = getEntityManager1();

        // Find the currently stored job and check it's work status
        if (getCurrentJob().getId() != null) {
            Job savedJob = Job.findJobById(em, getCurrentJob().getId());

            // Do not allow flagging job as completed unless job costing is approved
            if (!getCurrentJob().getJobCostingAndPayment().getCostingApproved()
                    && getCurrentJob().getJobStatusAndTracking().getWorkProgress().equals("Completed")) {

                PrimeFacesUtils.addMessage("Job Work Progress Cannot Be As Marked Completed",
                        "The job costing needs to be approved before this job can be marked as completed.",
                        FacesMessage.SEVERITY_WARN);

                return false;
            }

            if (savedJob.getJobStatusAndTracking().getWorkProgress().equals("Completed")
                    && !getUser().getPrivilege().getCanBeJMTSAdministrator()
                    && !getUser().isUserDepartmentSupervisor(getCurrentJob(), em)) {

                // Reset current job to its saved work progress
                getCurrentJob().getJobStatusAndTracking().
                        setWorkProgress(savedJob.getJobStatusAndTracking().getWorkProgress());

                PrimeFacesUtils.addMessage("Job Work Progress Cannot Be Changed",
                        "\"This job is marked as completed and cannot be changed. You may contact the department's supervisor.",
                        FacesMessage.SEVERITY_WARN);

                return false;
            } else if (savedJob.getJobStatusAndTracking().getWorkProgress().equals("Completed")
                    && (getUser().getPrivilege().getCanBeJMTSAdministrator()
                    || getUser().isUserDepartmentSupervisor(getCurrentJob(), em))) {
                // System admin can change work status even if it's completed.
                return true;
            } else if (!savedJob.getJobStatusAndTracking().getWorkProgress().equals("Completed")
                    && getCurrentJob().getJobStatusAndTracking().getWorkProgress().equals("Completed")
                    && !getCurrentJob().getJobCostingAndPayment().getCostingCompleted()) {

                // Reset current job to its saved work progress
                getCurrentJob().getJobStatusAndTracking().
                        setWorkProgress(savedJob.getJobStatusAndTracking().getWorkProgress());

                PrimeFacesUtils.addMessage("Job Work Progress Cannot Be As Marked Completed",
                        "The job costing needs to be prepared before this job can be marked as completed.",
                        FacesMessage.SEVERITY_WARN);

                return false;

            }
        } else {

            PrimeFacesUtils.addMessage("Job Work Progress Cannot be Changed",
                    "This job's work progress cannot be changed until the job is saved.",
                    FacesMessage.SEVERITY_WARN);
            return false;
        }

        return true;
    }

    public void updateWorkProgress() {

        if (checkWorkProgressReadinessToBeChanged()) {
            if (!currentJob.getJobStatusAndTracking().getWorkProgress().equals("Completed")) {
                currentJob.getJobStatusAndTracking().setCompleted(false);
                currentJob.getJobStatusAndTracking().setSamplesCollected(false);
                currentJob.getJobStatusAndTracking().setDocumentCollected(false);
                // overall job completion
                currentJob.getJobStatusAndTracking().setDateOfCompletion(null);
                currentJob.getJobStatusAndTracking().
                        setCompletedBy(null);
                // sample collection
                currentJob.getJobStatusAndTracking().setSamplesCollectedBy(null);
                currentJob.getJobStatusAndTracking().setDateSamplesCollected(null);
                // document collection
                currentJob.getJobStatusAndTracking().setDocumentCollectedBy(null);
                currentJob.getJobStatusAndTracking().setDateDocumentCollected(null);

                // Update start date
                if (currentJob.getJobStatusAndTracking().getWorkProgress().equals("Ongoing")
                        && currentJob.getJobStatusAndTracking().getStartDate() == null) {
                    currentJob.getJobStatusAndTracking().setStartDate(new Date());
                } else if (currentJob.getJobStatusAndTracking().getWorkProgress().equals("Not started")) {
                    currentJob.getJobStatusAndTracking().setStartDate(null);
                }

            } else {
                currentJob.getJobStatusAndTracking().setCompleted(true);
                currentJob.getJobStatusAndTracking().setDateOfCompletion(new Date());
                currentJob.getJobStatusAndTracking().
                        setCompletedBy(getUser().getEmployee());
            }

            setIsDirty(true);
        } else {
            if (getCurrentJob().getId() != null) {
                // Reset work progress to the currently saved state
                Job job = Job.findJobById(getEntityManager1(), getCurrentJob().getId());
                if (job != null) {
                    getCurrentJob().getJobStatusAndTracking().setWorkProgress(job.getJobStatusAndTracking().getWorkProgress());
                } else {
                    getCurrentJob().getJobStatusAndTracking().setWorkProgress("Not started");
                }
            } else {
                getCurrentJob().getJobStatusAndTracking().setWorkProgress("Not started");
            }
        }

    }

    public void resetCurrentJob() {
        EntityManager em = getEntityManager1();

        createJob(em, false);
    }

    public Boolean createJob(EntityManager em, Boolean isSubcontract) {

        try {
            if (isSubcontract) {

                // Save current job as parent job for use in the subcontract
                Job parent = currentJob;
                // Create copy of job and use current sequence number and year.                
                Long currentJobSequenceNumber = currentJob.getJobSequenceNumber();
                Integer yearReceived = currentJob.getYearReceived();
                currentJob = Job.copy(em, currentJob, getUser(), true, true);
                currentJob.setParent(parent);
                currentJob.setClassification(new Classification());
                currentJob.setSubContractedDepartment(new Department());
                currentJob.setIsToBeSubcontracted(isSubcontract);
                currentJob.setYearReceived(yearReceived);
                currentJob.setJobSequenceNumber(currentJobSequenceNumber);

            } else {
                currentJob = Job.create(em, getUser(), true);
            }
            if (currentJob == null) {
                PrimeFacesUtils.addMessage("Job NOT Created",
                        "An error occurred while creating a job. Try again or contact the System Administrator",
                        FacesMessage.SEVERITY_ERROR);
            } else {
                if (isSubcontract) {
                    setIsDirty(true);
                } else {
                    setIsDirty(false);
                }
            }

            getJobFinanceManager().setAccPacCustomer(new AccPacCustomer(""));

        } catch (Exception e) {
            System.out.println(e);
        }

        return true;
    }

    /**
     *
     * @param serviceRequest
     */
    public void createNewJob(ServiceRequest serviceRequest) {
        // handle user privilege and return if the user does not have
        // the privilege to do what they wish
        EntityManager em = getEntityManager1();
        createJob(em, false);

        // fill in fields from service request
        currentJob.setBusinessOffice(serviceRequest.getBusinessOffice());
        currentJob.setJobSequenceNumber(serviceRequest.getServiceRequestSequenceNumber());
        currentJob.getClient().doCopy(serviceRequest.getClient());
        currentJob.setDepartment(serviceRequest.getDepartment());
        currentJob.setAssignedTo(serviceRequest.getAssignedTo());
        if (currentJob.getAutoGenerateJobNumber()) {
            currentJob.setJobNumber(Job.getJobNumber(currentJob, em));
        }
        // set job dirty to ensure it is saved if attempt is made to close it
        //  before saving
        setIsDirty(true);
    }

    public void subContractJob(ActionEvent actionEvent) {
        EntityManager em = getEntityManager1();

        if (currentJob.getId() == null || currentJob.getIsDirty()) {
            PrimeFacesUtils.addMessage("Subcontract NOT Created",
                    "This job must be saved before it can be subcontracted",
                    FacesMessage.SEVERITY_ERROR);
            return;
        } else if (currentJob.getIsSubContract()) {
            PrimeFacesUtils.addMessage("Subcontract NOT Created",
                    "A subcontract cannot be subcontracted",
                    FacesMessage.SEVERITY_ERROR);
            return;
        }

        if (createJob(em, true)) {
            PrimeFacesUtils.addMessage("Job Copied for Subcontract",
                    "The current job was copied but the copy was not saved. "
                    + "Please enter or change the details for the copied job as required for the subcontract",
                    FacesMessage.SEVERITY_INFO);
        } else {
            PrimeFacesUtils.addMessage("Subcontract NOT Created",
                    "The subcontract was not created. Contact your System Administrator",
                    FacesMessage.SEVERITY_ERROR);
        }
    }

    public void cancelClientEdit(ActionEvent actionEvent) {
        if (currentJob.getClient().getId() == null) {
            currentJob.getClient().setName("");
        }
    }

    public String getSearchResultsTableHeader() {
        return ReportUtils.getSearchResultsTableHeader(getDateSearchPeriod(), getJobSearchResultList());
    }

    public void cancelJobEdit(ActionEvent actionEvent) {
        setIsDirty(false);
        PrimeFacesUtils.closeDialog(null);
        doJobSearch();
    }

    public void saveAndCloseCurrentJob() {
        saveCurrentJob();
        PrimeFacesUtils.closeDialog(null);
    }

    private void prepareAndSaveCurrentJob() {
        ReturnMessage returnMessage;

        returnMessage = getCurrentJob().prepareAndSave(getEntityManager1(), getUser());

        if (returnMessage.isSuccess()) {
            PrimeFacesUtils.addMessage("Saved!", "Job was saved", FacesMessage.SEVERITY_INFO);
            getCurrentJob().getJobStatusAndTracking().setEditStatus("        ");
        } else {
            PrimeFacesUtils.addMessage("Job NOT Saved!",
                    "Job was NOT saved. Please contact the System Administrator!",
                    FacesMessage.SEVERITY_ERROR);

            sendErrorEmail("An error occurred while saving a job!",
                    "Job number: " + getCurrentJob().getJobNumber()
                    + "\nJMTS User: " + getUser().getUsername()
                    + "\nDate/time: " + new Date()
                    + "\nDetail: " + returnMessage.getDetail());
        }
    }

    public void saveCurrentJob() {       
        // Ensure that at least 1 service is selected
        if (getCurrentJob().getServices().isEmpty()) {
            PrimeFacesUtils.addMessage("Service(s) NOT Selected",
                    "Please select at least one service",
                    FacesMessage.SEVERITY_ERROR);

            return;
        }

        // Check if there exists another subcontract with the same job number.
        Job savedSubcontract = Job.findJobByJobNumber(getEntityManager1(), getCurrentJob().getJobNumber());
        if (savedSubcontract != null && isCurrentJobNew() 
                && !savedSubcontract.getJobStatusAndTracking().getWorkProgress().equals("Cancelled")) {
            PrimeFacesUtils.addMessage("Subcontract already exists!",
                    "This subcontract cannot be saved because another subcontract already exists with the same job number",
                    FacesMessage.SEVERITY_ERROR);

            return;
        }

        // Do privelege checks and save if possible
        if (isCurrentJobNew() && getUser().getEmployee().getDepartment().getPrivilege().getCanEditJob()) {
            prepareAndSaveCurrentJob();
        } else if (isCurrentJobNew() && getUser().getEmployee().getDepartment().getPrivilege().getCanEnterJob()) {
            prepareAndSaveCurrentJob();
        } else if (isCurrentJobNew() && getUser().getPrivilege().getCanEnterJob()) {
            prepareAndSaveCurrentJob();
        } else if (isCurrentJobNew()
                && getUser().getPrivilege().getCanEnterDepartmentJob()
                && getUser().getEmployee().isMemberOf(Department.findDepartmentAssignedToJob(getCurrentJob(), getEntityManager1()))) {
            prepareAndSaveCurrentJob();
        } else if (isCurrentJobNew()
                && getUser().getPrivilege().getCanEnterOwnJob()
                && isCurrentJobJobAssignedToUser()) {
            prepareAndSaveCurrentJob();
        } else if (getIsDirty() && !isCurrentJobNew() && getUser().getPrivilege().getCanEditJob()) {
            prepareAndSaveCurrentJob();
        } else if (getIsDirty() && !isCurrentJobNew()
                && getUser().getPrivilege().getCanEditDepartmentJob()
                && (getUser().getEmployee().isMemberOf(Department.findDepartmentAssignedToJob(getCurrentJob(), getEntityManager1()))
                || getUser().getEmployee().isMemberOf(getCurrentJob().getDepartment()))) {
            prepareAndSaveCurrentJob();
        } else if (getIsDirty() && !isCurrentJobNew()
                && getUser().getPrivilege().getCanEditOwnJob()
                && isCurrentJobJobAssignedToUser()) {
            prepareAndSaveCurrentJob();
        } else if (getCurrentJob().getIsToBeCopied()) {
           prepareAndSaveCurrentJob();
        } else if (getCurrentJob().getIsToBeSubcontracted()) {
           prepareAndSaveCurrentJob();
        } else if (!getIsDirty()) {
            PrimeFacesUtils.addMessage("Already Saved",
                    "Job was not saved because it was not modified or it was recently saved.",
                    FacesMessage.SEVERITY_INFO);
        } else {
            PrimeFacesUtils.addMessage("Insufficient Privilege",
                    "You may not have the privilege to enter/save this job. \n"
                    + "Please contact the IT/MIS Department for further assistance.",
                    FacesMessage.SEVERITY_ERROR);
        }

    }

    public Boolean checkUserJobEntryPrivilege() {

        return getUser().getPrivilege().getCanEnterJob()
                || getUser().getPrivilege().getCanEnterDepartmentJob()
                //       || getUser().getDepartment().getPrivilege().getCanEnterJob()
                || getUser().getPrivilege().getCanEnterOwnJob();
    }

    public Boolean getIsClientNameValid() {
        return BusinessEntityUtils.validateName(currentJob.getClient().getName());
    }

    public Boolean getIsBillingAddressNameValid() {
        return BusinessEntityUtils.validateText(currentJob.getBillingAddress().getName());
    }

    /**
     * NB: Message body and subject are to be obtained from a "template". The
     * variables in the template are to be inserted where {variable} appears.
     *
     * @param job
     * @return
     */
    public String getNewJobEmailMessage(Job job) {
        String message = "";
        DateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

        message = message + "Dear Job Assignee,<br><br>";
        message = message + "A job with the following details was assigned to your department via the <a href='http://boshrmapp:8080/jmts'>Job Management & Tracking System (JMTS)</a>:<br><br>";
        message = message + "<span style='font-weight:bold'>Job number: </span>" + job.getJobNumber() + "<br>";
        message = message + "<span style='font-weight:bold'>Client: </span>" + job.getClient().getName() + "<br>";
        if (!job.getSubContractedDepartment().getName().equals("--")) {
            message = message + "<span style='font-weight:bold'>Department: </span>" + job.getSubContractedDepartment().getName() + "<br>";
        } else {
            message = message + "<span style='font-weight:bold'>Department: </span>" + job.getDepartment().getName() + "<br>";
        }
        message = message + "<span style='font-weight:bold'>Date submitted: </span>" + formatter.format(job.getJobStatusAndTracking().getDateSubmitted()) + "<br>";
        message = message + "<span style='font-weight:bold'>Current assignee: </span>" + BusinessEntityUtils.getPersonFullName(job.getAssignedTo(), Boolean.FALSE) + "<br>";
        message = message + "<span style='font-weight:bold'>Entered by: </span>" + BusinessEntityUtils.getPersonFullName(job.getJobStatusAndTracking().getEnteredBy(), Boolean.FALSE) + "<br>";
        message = message + "<span style='font-weight:bold'>Task/Sample descriptions: </span>" + job.getJobSampleDescriptions() + "<br><br>";
        message = message + "If you are the department's supervisor, you should immediately ensure that the job was correctly assigned to your staff member who will see to its completion.<br><br>";
        message = message + "If this job was incorrectly assigned to your department, the department supervisor should contact the person who entered/assigned the job.<br><br>";
        message = message + "This email was automatically generated and sent by the <a href='http://boshrmapp:8080/jmts'>JMTS</a>. Please DO NOT reply.<br><br>";
        message = message + "Signed<br>";
        message = message + "Job Manager";

        return message;
    }

    /**
     * NB: Message body and subject are to be obtained from a "template". The
     * variables in the template are to be inserted where {variable} appears.
     *
     * @param job
     * @return
     */
    public String getUpdatedJobEmailMessage(Job job) {
        String message = "";
        DateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

        message = message + "Dear Colleague,<br><br>";
        message = message + "A job with the following details was updated by someone outside of your department via the <a href='http://boshrmapp:8080/jmts'>Job Management & Tracking System (JMTS)</a>:<br><br>";
        message = message + "<span style='font-weight:bold'>Job number: </span>" + job.getJobNumber() + "<br>";
        message = message + "<span style='font-weight:bold'>Client: </span>" + job.getClient().getName() + "<br>";
        if (!job.getSubContractedDepartment().getName().equals("--")) {
            message = message + "<span style='font-weight:bold'>Department: </span>" + job.getSubContractedDepartment().getName() + "<br>";
        } else {
            message = message + "<span style='font-weight:bold'>Department: </span>" + job.getDepartment().getName() + "<br>";
        }
        message = message + "<span style='font-weight:bold'>Date submitted: </span>" + formatter.format(job.getJobStatusAndTracking().getDateSubmitted()) + "<br>";
        message = message + "<span style='font-weight:bold'>Current assignee: </span>" + BusinessEntityUtils.getPersonFullName(job.getAssignedTo(), Boolean.FALSE) + "<br>";
        message = message + "<span style='font-weight:bold'>Updated by: </span>" + BusinessEntityUtils.getPersonFullName(job.getJobStatusAndTracking().getEditedBy(), Boolean.FALSE) + "<br>";
        message = message + "<span style='font-weight:bold'>Task/Sample descriptions: </span>" + job.getJobSampleDescriptions() + "<br><br>";
        message = message + "You are being informed of this update so that you may take the requisite action.<br><br>";
        message = message + "This email was automatically generated and sent by the <a href='http://boshrmapp:8080/jmts'>JMTS</a>. Please DO NOT reply.<br><br>";
        message = message + "Signed<br>";
        message = message + "Job Manager";

        return message;
    }

    /**
     * Update/create alert for the current job if the job is not completed.
     *
     * @param em
     * @throws java.lang.Exception
     */
    public void updateAlert(EntityManager em) throws Exception {
        if (getCurrentJob().getJobStatusAndTracking().getCompleted() == null) {
            em.getTransaction().begin();

            Alert alert = Alert.findFirstAlertByOwnerId(em, currentJob.getId());
            if (alert == null) { // This seems to be a new job
                alert = new Alert(currentJob.getId(), new Date(), "Job entered");
                em.persist(alert);
            } else {
                em.refresh(alert);
                alert.setActive(true);
                alert.setDueTime(new Date());
                alert.setStatus("Job saved");
            }

            em.getTransaction().commit();
        } else if (!getCurrentJob().getJobStatusAndTracking().getCompleted()) {
            em.getTransaction().begin();

            Alert alert = Alert.findFirstAlertByOwnerId(em, currentJob.getId());
            if (alert == null) { // This seems to be a new job
                alert = new Alert(currentJob.getId(), new Date(), "Job saved");
                em.persist(alert);
            } else {
                em.refresh(alert);
                alert.setActive(true);
                alert.setDueTime(new Date());
                alert.setStatus("Job saved");
            }

            em.getTransaction().commit();
        }

    }

    public void sendErrorEmail(String subject, String message) {
        try {
            // send error message to developer's email            
            Utils.postMail(null, null, null, subject, message,
                    "text/plain", getEntityManager1());
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void editJob() {
        getCurrentJob().getJobStatusAndTracking().setEditStatus("        ");
        PrimeFacesUtils.openDialog(null, "jobDialog", true, true, true, 600, 975);
    }

    public void editJobCostingAndPayment() {
        PrimeFacesUtils.openDialog(null, "jobDialog", true, true, true, 600, 975);
    }

    public String getJobAssignee() {
        if (currentJob.getAssignedTo() != null) {
            return currentJob.getAssignedTo().getLastName() + ", " + currentJob.getAssignedTo().getFirstName();
        } else {
            return "";
        }
    }

    public String getCurrentJobNumber() {
        return Job.getJobNumber(currentJob, getEntityManager1());
    }

    public Date getJobSubmissionDate() {
        if (currentJob != null) {
            if (currentJob.getJobStatusAndTracking().getDateSubmitted() != null) {
                return currentJob.getJobStatusAndTracking().getDateSubmitted();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void setJobSubmissionDate(Date date) {
        currentJob.getJobStatusAndTracking().setDateSubmitted(date);
    }

    public void updateDateSubmitted() {

        EntityManager em = getEntityManager1();

        if (currentJob.getAutoGenerateJobNumber()) {
            currentJob.setJobNumber(Job.getJobNumber(currentJob, getEntityManager1()));
        }

        if (currentJob.getId() != null) {
            getJobFinanceManager().updateAllTaxes(null);
        }

        setIsDirty(true);
    }

    public void updateDateJobCompleted(SelectEvent event) {
        Date selectedDate = (Date) event.getObject();

        currentJob.getJobStatusAndTracking().setDateOfCompletion(selectedDate);

        setIsDirty(true);
    }

    public void updateDateExpectedCompletionDate(SelectEvent event) {
        Date selectedDate = (Date) event.getObject();

        currentJob.getJobStatusAndTracking().setExpectedDateOfCompletion(selectedDate);

        setIsDirty(Boolean.TRUE);
    }

    public List<Address> completeClientAddress(String query) {
        List<Address> addresses = new ArrayList<>();

        try {

            for (Address address : getCurrentJob().getClient().getAddresses()) {
                if (address.toString().toUpperCase().contains(query.toUpperCase())) {
                    addresses.add(address);
                }
            }

            return addresses;
        } catch (Exception e) {

            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<Contact> completeClientContact(String query) {
        List<Contact> contacts = new ArrayList<>();

        try {

            for (Contact contact : getCurrentJob().getClient().getContacts()) {
                if (contact.toString().toUpperCase().contains(query.toUpperCase())) {
                    contacts.add(contact);
                }
            }

            return contacts;
        } catch (Exception e) {
            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<Job> findJobs(Boolean includeSampleSearch) {
        return Job.findJobsByDateSearchField(getEntityManager1(),
                getUser(),
                getDateSearchPeriod(),
                getSearchType(),
                getSearchText(),
                includeSampleSearch);
    }

    public void doDefaultSearch() {

        switch (getDashboard().getSelectedTabId()) {
            case "Procurement":
                getPurchasingManager().doSearch();
                break;
            case "Financial Administration":
                getFinanceManager().doSearch();
                break;
            case "Document Management":
                getLegalDocumentManager().doSearch();
                break;
            case "Job Management":
                doSearch();
                break;
            default:
                break;
        }

    }

    public void doSearch() {

        doJobSearch();
        openJobBrowser();

//        switch (searchType) {
//            case "Purchase requisitions":
//                doPurchaseReqSearch();
//                break;
//            case "Suppliers":
////                getPurchasingManager().doSupplierSearch(searchText);
////                getPurchasingManager().openSuppliersTab();
//                break;
//            default:
//                doJobSearch();
//                openJobBrowser();
//                break;
//        }
    }

    public void doPurchaseReqSearch() {
//        getPurchasingManager().doPurchaseReqSearch(
//                getPurchasingManager().getDateSearchPeriod(),
//                getPurchasingManager().getSearchType(),
//                getPurchasingManager().getPurchaseReqSearchText(),
//                getUser().getEmployee().getDepartment().getId());
//
//        getPurchasingManager().openPurchaseReqsTab();
    }

    public void doJobSearch() {

        if (getUser().getId() != null) {
            jobSearchResultList = findJobs(false);

            if (jobSearchResultList.isEmpty()) {
                jobSearchResultList = findJobs(true);
            }

        } else {
            jobSearchResultList = new ArrayList<>();
        }

    }

    public void doJobSearch(DatePeriod dateSearchPeriod, String searchType, String searchText) {
        this.dateSearchPeriod = dateSearchPeriod;
        this.searchType = searchType;
        this.searchText = searchText;

        doJobSearch();
    }

    /**
     *
     * @return
     */
    public List<Classification> getActiveClassifications() {
        EntityManager em = getEntityManager1();

        List<Classification> classifications = Classification.findAllActiveClassifications(em);

        return classifications;
    }

    public List<Sector> getActiveSectors() {
        EntityManager em = getEntityManager1();

        List<Sector> sectors = Sector.findAllActiveSectors(em);

        return sectors;
    }

    public List<Address> getClientAddresses() {
        EntityManager em = getEntityManager1();

        List<Address> addresses = getCurrentJob().getClient().getAddresses();

        return addresses;
    }

    public List<JobCategory> getActiveJobCategories() {
        EntityManager em = getEntityManager1();

        List<JobCategory> categories = JobCategory.findAllActiveJobCategories(em);

        return categories;
    }

    public List<JobSubCategory> getActiveJobSubCategories() {
        EntityManager em = getEntityManager1();

        List<JobSubCategory> subCategories = JobSubCategory.findAllActiveJobSubCategories(em);

        return subCategories;
    }

    public List<Job> getJobSearchResultList() {
        return jobSearchResultList;
    }

    public /*synchronized*/ Job getCurrentJob() {
        if (currentJob == null) {
            resetCurrentJob();
        }
        return currentJob;
    }

    public /*synchronized*/ void setCurrentJob(Job currentJob) {
        this.currentJob = currentJob;
    }

    public void setEditCurrentJob(Job currentJob) {

        this.currentJob = currentJob;
        this.currentJob.setVisited(true);
        getJobFinanceManager().setEnableOnlyPaymentEditing(false);
    }

    public void setEditJobCosting(Job currentJob) {
        this.currentJob = currentJob;
        this.currentJob.setVisited(true);

        // Reload cash payments if possible to avoid overwriting them 
        // when saving
        EntityManager em = getEntityManager1();
        JobCostingAndPayment jcp
                = JobCostingAndPayment.findJobCostingAndPaymentById(em,
                        getCurrentJob().getJobCostingAndPayment().getId());

        em.refresh(jcp);

        currentJob.getJobCostingAndPayment().setCashPayments(jcp.getCashPayments());

        setSelectedJobs(new Job[]{});
    }

    public void setEditJobCostingAndPayment(Job currentJob) {
        this.currentJob = currentJob;
        this.currentJob.setVisited(true);

        getJobFinanceManager().setEnableOnlyPaymentEditing(true);
    }

    @Override
    public void setIsDirty(Boolean dirty) {
        getCurrentJob().setIsDirty(dirty);
        if (dirty) {
            getCurrentJob().getJobStatusAndTracking().setEditStatus("(edited)");
        } else {
            getCurrentJob().getJobStatusAndTracking().setEditStatus("        ");
        }
    }

    @Override
    public Boolean getIsDirty() {
        return getCurrentJob().getIsDirty();
    }

    public void updateSector() {
        setIsDirty(true);
    }

    public void updateBillingAddress() {
        setIsDirty(true);
    }

    public void updateDepartment() {

        EntityManager em;

        try {

            em = getEntityManager1();

            if (currentJob.getAutoGenerateJobNumber()) {
                currentJob.setJobNumber(getCurrentJobNumber());
            }

            if (currentJob.getId() != null) {
                getJobFinanceManager().updateAllTaxes(null);
            }

            setIsDirty(true);

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public void updateSubContractedDepartment() {
        EntityManager em;

        try {
            em = getEntityManager1();

            if (currentJob.getAutoGenerateJobNumber()) {
                currentJob.setJobNumber(getCurrentJobNumber());
            }

            if (currentJob.getId() != null) {
                getJobFinanceManager().updateAllTaxes(null);
            }

        } catch (Exception e) {
            System.out.println(e + ": updateSubContractedDepartment");
        }
    }

    /**
     * Do update for the client field on the General tab on the Job Details form
     */
    public void updateJobEntryTabClient() {

        getJobFinanceManager().getAccPacCustomer().setCustomerName(currentJob.getClient().getName());
        if (useAccPacCustomerList) {
            getJobFinanceManager().updateCreditStatus(null);
        }

        currentJob.setBillingAddress(new Address());
        currentJob.setContact(new Contact());

        // Set default tax
        if (currentJob.getClient().getDefaultTax().getId() != null) {
            currentJob.getJobCostingAndPayment().setTax(currentJob.getClient().getDefaultTax());
        }

        // Set default discount
        if (currentJob.getClient().getDiscount().getId() != null) {
            currentJob.getJobCostingAndPayment().setDiscount(currentJob.getClient().getDiscount());
        }

        setIsDirty(true);
    }

    public Job getSelectedJob() {
        if (selectedJob == null) {
            selectedJob = new Job();
            selectedJob.setJobNumber("");
        }
        return selectedJob;
    }

    public void setSelectedJob(Job selectedJob) {
        this.selectedJob = selectedJob;
    }

    public void createNewJobClient() {
        getClientManager().createNewClient(true);

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void editJobClient() {
        getClientManager().setSelectedClient(getCurrentJob().getClient());

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public ServiceRequest createNewServiceRequest(EntityManager em,
            User user,
            Boolean autoGenerateServiceRequestNumber) {

        ServiceRequest sr = new ServiceRequest();
        sr.setClient(new Client("", false));
        sr.setServiceRequestNumber("");
        sr.setJobDescription("");

        sr.setBusinessOffice(BusinessOffice.findDefaultBusinessOffice(em, "Head Office"));

        sr.setClassification(Classification.findClassificationByName(em, "--"));
        sr.setSector(Sector.findSectorByName(em, "--"));
        sr.setJobCategory(JobCategory.findJobCategoryByName(em, "--"));
        sr.setJobSubCategory(JobSubCategory.findJobSubCategoryByName(em, "--"));

        sr.setServiceContract(ServiceContract.create());
        sr.setAutoGenerateServiceRequestNumber(autoGenerateServiceRequestNumber);

        sr.setDateSubmitted(new Date());

        return sr;
    }

    public User createNewUser(EntityManager em) {
        User jmuser = new User();

        jmuser.setEmployee(Employee.findDefaultEmployee(em, "--", "--", true));

        return jmuser;
    }

    public EntityManagerFactory setupDatabaseConnection(String PU) {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory(PU);
            if (emf.isOpen()) {
                return emf;
            } else {
                return null;
            }
        } catch (Exception ex) {
            System.out.println(PU + " Connection failed: " + ex);
            return null;
        }
    }

    public HashMap<String, String> getConnectionProperties(
            String url,
            String driver,
            String username,
            String password) {

        // setup new database connection properties
        HashMap<String, String> prop = new HashMap<>();
        prop.put("javax.persistence.jdbc.user", username);
        prop.put("javax.persistence.jdbc.password", password);
        prop.put("javax.persistence.jdbc.url", url);
        prop.put("javax.persistence.jdbc.driver", driver);

        return prop;
    }

    public Date getCurrentDate() {
        return new Date();
    }

    public Long getJobCountByQuery(EntityManager em, String query) {
        try {
            return (Long) em.createQuery(query).getSingleResult();
        } catch (Exception e) {
            System.out.println(e);
            return 0L;
        }
    }

    public Long saveServiceContract(EntityManager em, ServiceContract serviceContract) {
        return BusinessEntityUtils.saveBusinessEntity(em, serviceContract);
    }

    public void postJobManagerMailToUser(
            Session mailSession,
            User user,
            String subject,
            String message) throws Exception {

        boolean debug = false;
        Message msg;
        EntityManager em = getEntityManager1();

        if (mailSession == null) {
            //Set the host smtp address
            Properties props = new Properties();
            String mailServer = (String) SystemOption.getOptionValueObject(getEntityManager1(), "mail.smtp.host");
            props.put("mail.smtp.host", mailServer);

            // create some properties and get the default Session
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(debug);
            msg = new MimeMessage(session);
        } else {
            msg = new MimeMessage(mailSession);
        }

        // set the from and to address
        String email = (String) SystemOption.getOptionValueObject(em, "jobManagerEmailAddress");
        String name = (String) SystemOption.getOptionValueObject(em, "jobManagerEmailName");
        InternetAddress addressFrom = new InternetAddress(email, name); // option job manager email addres
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[1];
        if (user != null) {
            addressTo[0] = new InternetAddress(user.getUsername(), user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName());
        } else {
            String email1 = (String) SystemOption.getOptionValueObject(em, "administratorEmailAddress");
            String name1 = (String) SystemOption.getOptionValueObject(em, "administratorEmailName");
            addressTo[0] = new InternetAddress(email1, name1);
        }

        msg.setRecipients(Message.RecipientType.TO, addressTo);

        // Setting the Subject and Content Type
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        Transport.send(msg);
    }

    public void postJobManagerMail(
            Session mailSession,
            String addressedTo,
            String fullNameOfAddressedTo,
            String subject,
            String message) throws Exception {

        boolean debug = false;
        Message msg;
        EntityManager em = getEntityManager1();

        try {
            if (mailSession == null) {
                //Set the host smtp address
                Properties props = new Properties();
                String mailServer = (String) SystemOption.getOptionValueObject(em, "mail.smtp.host");
                String trust = (String) SystemOption.getOptionValueObject(em, "mail.smtp.ssl.trust");
                props.put("mail.smtp.host", mailServer);
                props.setProperty("mail.smtp.ssl.trust", trust);

                // create some properties and get the default Session
                Session session = Session.getDefaultInstance(props, null);
                session.setDebug(debug);
                msg = new MimeMessage(session);
            } else {
                msg = new MimeMessage(mailSession);
            }

            // set the from and to address
            String email = (String) SystemOption.getOptionValueObject(em, "jobManagerEmailAddress");
            String name = (String) SystemOption.getOptionValueObject(em, "jobManagerEmailName");
            InternetAddress addressFrom = new InternetAddress(email, name);
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[1];

            addressTo[0] = new InternetAddress(addressedTo, fullNameOfAddressedTo);

            msg.setRecipients(Message.RecipientType.TO, addressTo);

            // Setting the Subject and Content Type
            msg.setSubject(subject);
            msg.setContent(message, "text/html; charset=utf-8");

            Transport.send(msg);
        } catch (UnsupportedEncodingException | MessagingException e) {
            System.out.println(e);
        }
    }

    public JobDataModel getJobsModel() {
        return new JobDataModel(jobSearchResultList);
    }

    public Boolean getDisableDepartment() {
        //return getCurrentJob().getIsToBeSubcontracted() || getCurrentJob().getIsSubContract();
        return getRenderSubContractingDepartment();
    }

    public Boolean getRenderSubContractingDepartment() {
        return getCurrentJob().getIsToBeSubcontracted() || getCurrentJob().getIsSubContract();
    }

    /**
     * This is to be implemented further
     *
     * @return
     */
    public Boolean getDisableSubContracting() {
        try {
            if (getCurrentJob().getIsSubContract() || getCurrentJob().getIsToBeCopied()) {
                return false;
            } else {
                return getCurrentJob().getId() == null;
            }
        } catch (Exception e) {
            System.out.println(e + ": getDisableSubContracting");
        }

        return false;
    }

    public Boolean isCurrentJobNew() {
        return (getCurrentJob().getId() == null);
    }

    public void generateEmailAlerts() {

        EntityManager em = getEntityManager1();
        // Post job mail to the department if this is a new job and wasn't entered by 
        // by a person assignment 
        try {
            // Do not sent job email if user is a member of the department to which the job was assigned
            if (isCurrentJobNew() && !getUser().getEmployee().isMemberOf(Department.findDepartmentAssignedToJob(currentJob, em))) {
                List<String> emails = Employee.getDepartmentSupervisorsEmailAddresses(Department.findDepartmentAssignedToJob(currentJob, em), em);
                emails.add(Employee.findEmployeeDefaultEmailAdress(currentJob.getAssignedTo(), getEntityManager1()));
                for (String email : emails) {
                    if (!email.equals("")) {
                        postJobManagerMail(null, email, "", "New Job Assignment", getNewJobEmailMessage(currentJob));
                    } else {
                        sendErrorEmail("The department's head email address is not valid!",
                                "Job number: " + currentJob.getJobNumber()
                                + "\nJMTS User: " + getUser().getUsername()
                                + "\nDate/time: " + new Date());
                    }
                }

            } else if (getIsDirty() && !getUser().getEmployee().isMemberOf(Department.findDepartmentAssignedToJob(currentJob, em))) {
                List<String> emails = Employee.getDepartmentSupervisorsEmailAddresses(Department.findDepartmentAssignedToJob(currentJob, em), em);
                emails.add(Employee.findEmployeeDefaultEmailAdress(currentJob.getAssignedTo(), getEntityManager1()));
                for (String email : emails) {
                    if (!email.equals("")) {
                        postJobManagerMail(null, email, "", "Job Update Alert", getUpdatedJobEmailMessage(currentJob));
                    } else {
                        sendErrorEmail("The department's head email address is not valid!",
                                "Job number: " + currentJob.getJobNumber()
                                + "\nJMTS User: " + getUser().getUsername()
                                + "\nDate/time: " + new Date());
                    }
                }

            }

        } catch (Exception e) {
            System.out.println("Error generating email alert");
            System.out.println(e);
        }

    }

    public void openClientsTab() {

        getSystemManager().getMainTabView().openTab("Clients");
    }

    public void openReportsTab() {
        getReportManager().openReportsTab("Job");
    }

    /**
     * Gets the SystemManager object as a session bean.
     *
     * @return
     */
    public SystemManager getSystemManager() {

        return BeanUtils.findBean("systemManager");
    }

    private void initMainTabView() {

        if (getUser().getModules().getJobManagementAndTrackingModule()) {
            getMainTabView().openTab("Job Browser");
        }

    }

    private void initDashboard() {

        if (getUser().getModules().getJobManagementAndTrackingModule()) {
            getSystemManager().getDashboard().openTab("Job Management");
        }
    }

    @Override
    public void completeLogin() {
        initDashboard();
        initMainTabView();
    }

    @Override
    public void completeLogout() {
        reset();
    }

}
