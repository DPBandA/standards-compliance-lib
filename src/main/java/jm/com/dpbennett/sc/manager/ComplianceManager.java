/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jm.com.dpbennett.sc.manager;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import jm.com.dpbennett.business.entity.dm.DocumentStandard;
import jm.com.dpbennett.business.entity.sm.Category;
import jm.com.dpbennett.business.entity.hrm.Address;
import jm.com.dpbennett.business.entity.hrm.Contact;
import jm.com.dpbennett.business.entity.hrm.Employee;
import jm.com.dpbennett.business.entity.hrm.User;
import jm.com.dpbennett.business.entity.jmts.Job;
import jm.com.dpbennett.business.entity.rm.DatePeriod;
import jm.com.dpbennett.business.entity.sc.CompanyRegistration;
import jm.com.dpbennett.business.entity.sc.ComplianceDailyReport;
import jm.com.dpbennett.business.entity.sc.ComplianceSurvey;
import jm.com.dpbennett.business.entity.sc.Distributor;
import jm.com.dpbennett.business.entity.sc.DocumentInspection;
import jm.com.dpbennett.business.entity.sc.ProductInspection;
import jm.com.dpbennett.business.entity.sc.ShippingContainer;
import jm.com.dpbennett.business.entity.hrm.Manufacturer;
import jm.com.dpbennett.business.entity.sc.Complaint;
import jm.com.dpbennett.business.entity.sc.FactoryInspection;
import jm.com.dpbennett.business.entity.sc.FactoryInspectionComponent;
import jm.com.dpbennett.business.entity.sc.MarketProduct;
import jm.com.dpbennett.business.entity.sm.SequenceNumber;
import jm.com.dpbennett.business.entity.sm.SystemOption;
import jm.com.dpbennett.business.entity.util.BusinessEntityUtils;
import jm.com.dpbennett.business.entity.util.ReturnMessage;
import jm.com.dpbennett.cm.manager.ClientManager;
import jm.com.dpbennett.hrm.manager.HumanResourceManager;
import jm.com.dpbennett.sm.Authentication.AuthenticationListener;
import jm.com.dpbennett.sm.manager.SystemManager;
import static jm.com.dpbennett.sm.manager.SystemManager.getStringListAsSelectItems;
import jm.com.dpbennett.sm.util.BeanUtils;
import jm.com.dpbennett.sm.util.PrimeFacesUtils;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Desmond Bennett
 */
public class ComplianceManager implements Serializable, AuthenticationListener {

    @PersistenceUnit(unitName = "JMTSPU")
    private EntityManagerFactory EMF1;
    private EntityManager entityManager1;
    private ComplianceSurvey currentComplianceSurvey;
    private ProductInspection currentProductInspection;
    private CompanyRegistration currentCompanyRegistration;
    private DocumentStandard currentDocumentStandard;
    private MarketProduct currentMarketProduct;
    private Complaint currentComplaint;
    private FactoryInspection currentFactoryInspection;
    private FactoryInspectionComponent currentFactoryInspectionComponent;
    private List<ComplianceSurvey> complianceSurveys;
    private List<DocumentStandard> documentStandards;
    private List<MarketProduct> marketProducts;
    private List<Complaint> complaints;
    private List<FactoryInspection> factoryInspections;
    private Date reportStartDate;
    private Date reportEndDate;
    private String surveySearchText;
    private String standardSearchText;
    private String marketProductSearchText;
    private String complaintSearchText;
    private String reportSearchText;
    private String factoryInspectionSearchText;
    private String dateSearchField;
    private String dateSearchPeriod;
    private String reportPeriod;
    private String searchType;
    private String dialogMessage;
    private String dialogMessageHeader;
    private String dialogMessageSeverity;
    private Integer longProcessProgress = 0;
    private ComplianceDailyReport currentComplianceDailyReport;
    private ShippingContainer currentShippingContainer;
    private DocumentInspection currentDocumentInspection;
    private List<DocumentInspection> documentInspections;
    private DatePeriod datePeriod;
    private List<String> selectedStandardNames;
    private String selectedFactoryInspectionTemplate;
    private List<String> selectedContainerNumbers;
    private String componentsToUpdate;
    private String shippingContainerTableToUpdate;
    private String complianceSurveyTableToUpdate;
    private Boolean isActiveDocumentStandardsOnly;
    private Boolean isActiveMarketProductsOnly;
    private Boolean edit;

    /**
     * Creates a new instance of ComplianceManager.
     */
    public ComplianceManager() {
        init();
    }

    private void init() {
        reset();

        getSystemManager().addSingleAuthenticationListener(this);
    }

    public FactoryInspectionComponent getCurrentFactoryInspectionComponent() {
        return currentFactoryInspectionComponent;
    }

    public void setCurrentFactoryInspectionComponent(FactoryInspectionComponent currentFactoryInspectionComponent) {
        this.currentFactoryInspectionComponent = currentFactoryInspectionComponent;
    }

    public void cancelFactoryInspectionComponentEdit() {
        currentFactoryInspectionComponent.setIsDirty(false);
    }

    public void createNewFactoryInspectionComponent(ActionEvent event) {
        currentFactoryInspectionComponent = new FactoryInspectionComponent();
        setEdit(false);
    }

    public void okFactoryInspectionComponent() {
        if (currentFactoryInspectionComponent.getId() == null && !getEdit()) {
            getCurrentFactoryInspection().getInspectionComponents().add(currentFactoryInspectionComponent);
        }

        setEdit(false);

        //PrimeFaces.current().executeScript("PF('factoryInspectionComponentDialog').hide();");
    }

    public void deleteFactoryInspectionComponent() {
        deleteFactoryInspectionComponentByName(currentFactoryInspectionComponent.getName());
    }

    public void deleteFactoryInspectionComponentByName(String componentName) {

        List<FactoryInspectionComponent> components = getCurrentFactoryInspection().getAllSortedFactoryInspectionComponents();
        int index = 0;
        for (FactoryInspectionComponent factoryInspectionComponent : components) {
            if (factoryInspectionComponent.getName().equals(componentName)) {
                components.remove(index);

                updateFactoryInspection();

                break;
            }
            ++index;
        }

    }

    public void editFactoryInspectionComponent(ActionEvent event) {
        setEdit(true);
    }

    public List<FactoryInspection> completeFactoryInspectionName(String query) {
        EntityManager em;

        try {
            em = getEntityManager1();

            List<FactoryInspection> results = FactoryInspection.findFactoryInspectionsByName(em, query);

            return results;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public void updateInspectionComponents() {
        if (selectedFactoryInspectionTemplate != null) {
            EntityManager em = getEntityManager1();
            FactoryInspection factoryInspection
                    = FactoryInspection.findFactoryInspectionByName(em, selectedFactoryInspectionTemplate);

            if (factoryInspection != null) {
                getCurrentFactoryInspection().getInspectionComponents().clear();
                getCurrentFactoryInspection().setInspectionComponents(copyFactoryInspectionComponents(factoryInspection.getInspectionComponents()));

                updateFactoryInspection();
            }

            selectedFactoryInspectionTemplate = null;

        }
    }

    public List<FactoryInspectionComponent> copyFactoryInspectionComponents(List<FactoryInspectionComponent> srcFactoryInspectionComponents) {
        ArrayList<FactoryInspectionComponent> newFactoryInspectionComponents = new ArrayList<>();

        for (FactoryInspectionComponent factoryInspectionComponent : srcFactoryInspectionComponents) {
            newFactoryInspectionComponents.add(new FactoryInspectionComponent(factoryInspectionComponent));
        }

        return newFactoryInspectionComponents;
    }

    public String getSelectedFactoryInspectionTemplate() {
        return selectedFactoryInspectionTemplate;
    }

    public void setSelectedFactoryInspectionTemplate(String selectedFactoryInspectionTemplate) {
        this.selectedFactoryInspectionTemplate = selectedFactoryInspectionTemplate;
    }

    public String getFactoryInspectionSearchText() {
        return factoryInspectionSearchText;
    }

    public void setFactoryInspectionSearchText(String factoryInspectionSearchText) {
        this.factoryInspectionSearchText = factoryInspectionSearchText;
    }

    public MarketProduct getCurrentMarketProduct() {
        return currentMarketProduct;
    }

    public void setCurrentMarketProduct(MarketProduct currentMarketProduct) {
        this.currentMarketProduct = currentMarketProduct;
    }

    public String getMarketProductSearchText() {
        return marketProductSearchText;
    }

    public void setMarketProductSearchText(String marketProductSearchText) {
        this.marketProductSearchText = marketProductSearchText;
    }

    public List<MarketProduct> getMarketProducts() {
        if (marketProducts == null) {
            marketProducts = MarketProduct.findAllActiveMarketProducts(getEntityManager1());
        }
        return marketProducts;
    }

    public void setMarketProducts(List<MarketProduct> marketProducts) {
        this.marketProducts = marketProducts;
    }

    public Boolean getEdit() {
        return edit;
    }

    public void setEdit(Boolean edit) {
        this.edit = edit;
    }

    public List<Contact> completeConsigneeRepresentative(String query) {
        List<Contact> contacts = new ArrayList<>();

        try {

            for (Contact contact : getCurrentComplianceSurvey().getConsignee().getContacts()) {
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

    public List<Contact> completeFactoryRepresentative(String query) {
        List<Contact> contacts = new ArrayList<>();

        try {

            for (Contact contact : getCurrentFactoryInspection().getManufacturer().getContacts()) {
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

    public List<Contact> completeBrokerRepresentative(String query) {
        List<Contact> contacts = new ArrayList<>();

        try {

            for (Contact contact : getCurrentComplianceSurvey().getBroker().getContacts()) {
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

    public List<Address> completeBrokerAddress(String query) {
        List<Address> addresses = new ArrayList<>();

        try {

            for (Address address : getCurrentComplianceSurvey().getBroker().getAddresses()) {
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

    public List<Contact> completeRetailRepresentative(String query) {
        List<Contact> contacts = new ArrayList<>();

        try {

            for (Contact contact : getCurrentComplianceSurvey().getRetailOutlet().getContacts()) {
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

    public void editConsignee() {
        getClientManager().setSelectedClient(getCurrentComplianceSurvey().getConsignee());
        getClientManager().setClientDialogTitle("Consignee Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void editComplainant() {
        getClientManager().setSelectedClient(getCurrentComplaint().getComplainant());
        getClientManager().setClientDialogTitle("Complainant Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void editReceivedVia() {
        getClientManager().setSelectedClient(getCurrentComplaint().getReceivedVia());
        getClientManager().setClientDialogTitle("Client Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void editBroker() {
        getClientManager().setSelectedClient(getCurrentComplianceSurvey().getBroker());
        getClientManager().setClientDialogTitle("Broker Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void editManufacturer() {
        getHumanResourceManager().setSelectedManufacturer(getCurrentProductInspection().getManufacturer());

        PrimeFacesUtils.openDialog(null, "/hr/manufacturer/manufacturerDialog", true, true, true, 450, 700);
    }

    public void editFactoryInspectionManufacturer() {
        getHumanResourceManager().setSelectedManufacturer(getCurrentFactoryInspection().getManufacturer());

        PrimeFacesUtils.openDialog(null, "/hr/manufacturer/manufacturerDialog", true, true, true, 450, 700);
    }

    public void editDistributor() {
        getClientManager().setSelectedClient(getCurrentProductInspection().getDistributor());
        getClientManager().setClientDialogTitle("Distributor Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void editRetailOutlet() {
        getClientManager().setSelectedClient(getCurrentComplianceSurvey().getRetailOutlet());
        getClientManager().setClientDialogTitle("Retail Outlet Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void createNewConsignee() {
        getClientManager().createNewClient(true);
        getClientManager().setClientDialogTitle("Consignee Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void createNewComplainant() {
        getClientManager().createNewClient(true);
        getClientManager().setClientDialogTitle("Complainant Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void createNewReceivedVia() {
        getClientManager().createNewClient(true);
        getClientManager().setClientDialogTitle("Client Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void createNewBroker() {
        getClientManager().createNewClient(true);
        getClientManager().setClientDialogTitle("Broker Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void createNewDistributor() {
        getClientManager().createNewClient(true);
        getClientManager().setClientDialogTitle("Distributor Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void createNewManufacturer() {
        getHumanResourceManager().createNewManufacturer(true);

        PrimeFacesUtils.openDialog(null, "/hr/manufacturer/manufacturerDialog", true, true, true, 450, 700);
    }

    public void createNewRetailOutlet() {
        getClientManager().createNewClient(true);
        getClientManager().setClientDialogTitle("Retail Outlet Detail");

        PrimeFacesUtils.openDialog(null, "/client/clientDialog", true, true, true, 450, 700);
    }

    public void consigneeDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentComplianceSurvey().setConsignee(getClientManager().getSelectedClient());
        }
    }

    public void complainantDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentComplaint().setComplainant(getClientManager().getSelectedClient());
        }
    }

    public void receivedViaDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentComplaint().setReceivedVia(getClientManager().getSelectedClient());
        }
    }

    public void brokerDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentComplianceSurvey().setBroker(getClientManager().getSelectedClient());
        }
    }

    public void distributorDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentProductInspection().setDistributor(getClientManager().getSelectedClient());
        }
    }

    public void manufacturerDialogReturn() {
        if (getHumanResourceManager().getSelectedManufacturer().getId() != null) {
            getCurrentProductInspection().setManufacturer(getHumanResourceManager().getSelectedManufacturer());
        }
    }

    public void factoryInspectionManufacturerDialogReturn() {
        if (getHumanResourceManager().getSelectedManufacturer().getId() != null) {
            getCurrentFactoryInspection().setManufacturer(getHumanResourceManager().getSelectedManufacturer());
        }
        
        getCurrentFactoryInspection().setAddress(new Address());
        getCurrentFactoryInspection().setFactoryRepresentative(new Contact());
    }

    public void retailOutletDialogReturn() {
        if (getClientManager().getSelectedClient().getId() != null) {
            getCurrentComplianceSurvey().setRetailOutlet(getClientManager().getSelectedClient());
        }
    }

    public Boolean getIsConsigneeNameValid() {
        return BusinessEntityUtils.validateName(currentComplianceSurvey.getConsignee().getName());
    }

    public Boolean getIsComplainantNameValid() {
        return BusinessEntityUtils.validateName(currentComplaint.getComplainant().getName());
    }

    public Boolean getIsReceivedViaNameValid() {
        return BusinessEntityUtils.validateName(currentComplaint.getReceivedVia().getName());
    }

    public Boolean getIsBrokerNameValid() {
        return BusinessEntityUtils.validateName(currentComplianceSurvey.getBroker().getName());
    }

    public Boolean getIsManufacturerNameValid() {
        return BusinessEntityUtils.validateName(currentProductInspection.getManufacturer().getName());
    }

    public Boolean getIsFactoryInspectionManufacturerNameValid() {
        return BusinessEntityUtils.validateName(currentFactoryInspection.getManufacturer().getName());
    }

    public Boolean getIsDistributorNameValid() {
        return BusinessEntityUtils.validateName(currentProductInspection.getDistributor().getName());
    }

    public Boolean getIsRetailOutletNameValid() {
        return BusinessEntityUtils.validateName(currentComplianceSurvey.getRetailOutlet().getName());
    }

    public List<String> getAllDocumentStandardNames() {
        EntityManager em = getEntityManager1();

        List<String> names = new ArrayList<>();

        List<DocumentStandard> standards = DocumentStandard.findAllDocumentStandards(em);
        for (DocumentStandard documentStandard : standards) {
            names.add(documentStandard.getName());
        }

        return names;
    }

    public String getComplianceSurveyTableToUpdate() {
        return complianceSurveyTableToUpdate;
    }

    public void setComplianceSurveyTableToUpdate(String complianceSurveyTableToUpdate) {
        this.complianceSurveyTableToUpdate = complianceSurveyTableToUpdate;
    }

    public String getShippingContainerTableToUpdate() {
        return shippingContainerTableToUpdate;
    }

    public void setShippingContainerTableToUpdate(String shippingContainerTableToUpdate) {
        this.shippingContainerTableToUpdate = shippingContainerTableToUpdate;
    }

    public String getComponentsToUpdate() {
        return componentsToUpdate;
    }

    public void setComponentsToUpdate(String componentsToUpdate) {
        this.componentsToUpdate = componentsToUpdate;
    }

    public List<String> getSelectedContainerNumbers() {
        return selectedContainerNumbers;
    }

    public void setSelectedContainerNumbers(List<String> selectedContainerNumbers) {
        this.selectedContainerNumbers = selectedContainerNumbers;
    }

    public List<String> getAllShippingContainers() {
        return getCurrentComplianceSurvey().getEntryDocumentInspection().getContainerNumberList();
    }

    public List<SelectItem> getProductCategories() {
        ArrayList types = new ArrayList();

        types.add(new SelectItem("", ""));
        List<Category> categories = Category.findCategoriesByType(getEntityManager1(), "Product");
        for (Category category : categories) {
            types.add(new SelectItem(category.getName(), category.getName()));
        }

        return types;
    }

    public String getTablesToUpdateAfterSearch() {

        return ":mainTabViewForm:mainTabView:complianceSurveysTable,:mainTabViewForm:mainTabView:documentInspectionsTable";
    }

//    public String getProductTablesToUpdate() {
//
//        return ":ComplianceSurveyDialogForm:complianceSurveyTabView:marketProductsTable";
//    }
    public List<SelectItem> getDocumentStamps() {
        ArrayList stamps = new ArrayList();
        stamps.add(new SelectItem("", ""));
        stamps.addAll(getStringListAsSelectItems(getEntityManager1(), "portOfEntryDocumentStampList"));

        return stamps;
    }

    public List<String> completeJobNumber(String query) {
        List<String> jobNumbers = new ArrayList<>();

        try {

            List<Job> foundJobs = Job.findAllByJobNumber(getEntityManager1(), query);

            for (Job job : foundJobs) {
                jobNumbers.add(job.getJobNumber());
            }

            return jobNumbers;

        } catch (Exception e) {
            System.out.println(e);
            return new ArrayList<>();
        }
    }

    public List<SelectItem> getSurveyLocationTypes() {
        ArrayList types = new ArrayList();

        types.addAll(getStringListAsSelectItems(getEntityManager1(), "complianceSurveyLocationTypes"));

        return types;
    }

    public List getTypesOfEstablishment() {
        ArrayList types = new ArrayList();

        types.add(new SelectItem(" ", " "));
        switch (getCurrentComplianceSurvey().getSurveyLocationType()) {
            case "Site":
                types.addAll(getStringListAsSelectItems(getEntityManager1(), "siteTypesOfEstablishment"));
                break;
            case "Commercial Marketplace":
                types.addAll(getStringListAsSelectItems(getEntityManager1(), "commercialTypesOfEstablishment"));
                break;
        }

        return types;
    }

    public List<SelectItem> getTypesOfPortOfEntry() {
        return getStringListAsSelectItems(getEntityManager1(), "portOfEntryTypeList");
    }

    public List<String> getSelectedStandardNames() {
        return selectedStandardNames;
    }

    public void setSelectedStandardNames(List<String> selectedStandardNames) {
        this.selectedStandardNames = selectedStandardNames;
    }

    public void reset() {

        documentInspections = new ArrayList<>();
        dateSearchField = "dateOfSurvey";
        surveySearchText = "";
        standardSearchText = "";
        complaintSearchText = "";
        marketProductSearchText = "";
        factoryInspectionSearchText = "";
        searchType = "General";
        dateSearchPeriod = "This month";
        reportPeriod = "This month";
        datePeriod = new DatePeriod("This month", "month", null, null, null, null, false, false, false);
        datePeriod.initDatePeriod();
        componentsToUpdate = ":ComplianceSurveyDialogForm";
        complianceSurveyTableToUpdate = "mainTabViewForm:mainTabView:complianceSurveysTable";
        isActiveDocumentStandardsOnly = true;
        isActiveMarketProductsOnly = true;
    }

    public List<FactoryInspection> getFactoryInspections() {
        if (factoryInspections == null) {
            doFactoryInspectionSearch();
        }
        return factoryInspections;
    }

    public void setFactoryInspections(List<FactoryInspection> factoryInspections) {
        this.factoryInspections = factoryInspections;
    }

    public void onFactoryInspectionCellEdit(CellEditEvent event) {
        BusinessEntityUtils.saveBusinessEntityInTransaction(getEntityManager1(),
                getFactoryInspections().get(event.getRowIndex()));
    }

    public int getNumFactoryInspectionsFound() {
        return getFactoryInspections().size();
    }

    public void editCurrentFactoryInspection() {
       editFactoryInspection(); 
    }

    public Boolean getIsActiveMarketProductsOnly() {
        return isActiveMarketProductsOnly;
    }

    public void setIsActiveMarketProductsOnly(Boolean isActiveMarketProductsOnly) {
        this.isActiveMarketProductsOnly = isActiveMarketProductsOnly;
    }

    public Boolean getIsActiveDocumentStandardsOnly() {
        return isActiveDocumentStandardsOnly;
    }

    public void setIsActiveDocumentStandardsOnly(Boolean isActiveDocumentStandardsOnly) {
        this.isActiveDocumentStandardsOnly = isActiveDocumentStandardsOnly;
    }

    /**
     * Gets the SystemManager object as a session bean.
     *
     * @return
     */
    public SystemManager getSystemManager() {

        return BeanUtils.findBean("systemManager");
    }

    /**
     * Gets the title of the application which may be saved in a database.
     *
     * @return
     */
    public String getTitle() {
        return "Standards Compliance";
    }

    public List<Manufacturer> completeManufacturer(String query) {
        return Manufacturer.findManufacturersBySearchPattern(getEntityManager1(), query);
    }

    public void updateConsignee() {
        currentComplianceSurvey.setConsigneeRepresentative(new Contact());
        currentComplianceSurvey.setIsDirty(true);
    }

    public void updateComplainant() {
        currentComplaint.setIsDirty(true);
    }

    public void updateBroker() {
        currentComplianceSurvey.setBrokerRepresentative(new Contact());
        currentComplianceSurvey.setBrokerAddress(new Address());
        currentComplianceSurvey.setIsDirty(true);
    }

    public void updateRetailOutlet() {
        currentComplianceSurvey.setRetailRepresentative(new Contact());
        currentComplianceSurvey.setIsDirty(true);
    }

    public void openComplianceSurvey() {
        PrimeFacesUtils.openDialog(null, "/compliance/surveyDialog", true, true, true, true, 650, 800);
    }

    public void editFactoryInspection() {
        PrimeFacesUtils.openDialog(null, "/compliance/factoryInspectionDialog", true, true, true, true, 650, 800);
    }

    public void openProductInspectionDialog() {
        PrimeFacesUtils.openDialog(null, "/compliance/productInspectionDialog", true, true, true, true, 650, 800);
    }

    public void openComplaintProductInspectionDialog() {
        PrimeFacesUtils.openDialog(null, "/compliance/complaintProductInspectionDialog", true, true, true, true, 650, 800);
    }

    public void openDocumentStandardDialog() {
        PrimeFacesUtils.openDialog(null, "/compliance/documentStandardDialog", true, true, true, true, 650, 800);
    }

    public void openSurveysBrowser() {

        getSystemManager().getMainTabView().openTab("Survey Browser");
    }

    public void openStandardsBrowser() {

        getSystemManager().getMainTabView().openTab("Standard Browser");
    }

    public void openComplaintsBrowser() {

        getSystemManager().getMainTabView().openTab("Complaint Browser");
    }

    public HumanResourceManager getHumanResourceManager() {

        return BeanUtils.findBean("humanResourceManager");
    }

    public ClientManager getClientManager() {

        return BeanUtils.findBean("clientManager");
    }

    public void surveyDialogReturn() {
        doSurveySearch();
    }

    public void complaintDialogReturn() {
        doComplaintSearch();
    }

    public void updateDatePeriodSearch() {
        getDatePeriod().initDatePeriod();

    }

    public DatePeriod getDatePeriod() {
        return datePeriod;
    }

    public void setDatePeriod(DatePeriod datePeriod) {
        this.datePeriod = datePeriod;
    }

    public List getComplianceSearchTypes() {
        ArrayList searchTypes = new ArrayList();

        searchTypes.add(new SelectItem("General", "General"));

        return searchTypes;
    }

    public List getComplianceDateSearchFields() {
        ArrayList dateFields = new ArrayList();

        dateFields.add(new SelectItem("dateOfSurvey", "Date of survey"));

        return dateFields;
    }

    public void updateSearch() {
    }

    public List<DocumentInspection> getDocumentInspections() {
        return documentInspections;
    }

    public DocumentInspection getCurrentDocumentInspection() {
        if (currentDocumentInspection == null) {
            currentDocumentInspection = new DocumentInspection();
        }
        return currentDocumentInspection;
    }

    public void setCurrentDocumentInspection(DocumentInspection currentDocumentInspection) {
        this.currentDocumentInspection = currentDocumentInspection;
    }

    public ShippingContainer getCurrentShippingContainer() {
        if (currentShippingContainer == null) {
            currentShippingContainer = new ShippingContainer();
        }
        return currentShippingContainer;
    }

    public void setCurrentShippingContainer(ShippingContainer currentShippingContainer) {
        this.currentShippingContainer = currentShippingContainer;
    }

    public StreamedContent getAuthSigForDetentionRequestPOE() {
        if (currentComplianceSurvey.getAuthSigForDetentionRequestPOE().getId() != null) {
            if (currentComplianceSurvey.getAuthSigForDetentionRequestPOE().getSignatureImage() != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(currentComplianceSurvey.getAuthSigForDetentionRequestPOE().getSignatureImage()), "image/png");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public StreamedContent getInspectorSigForSampleRequestPOE() {
        if (currentComplianceSurvey.getInspectorSigForSampleRequestPOE().getId() != null) {
            if (currentComplianceSurvey.getInspectorSigForSampleRequestPOE().getSignatureImage() != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(currentComplianceSurvey.getInspectorSigForSampleRequestPOE().getSignatureImage()), "image/png");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public StreamedContent getPreparedBySigForReleaseRequestPOE() {
        if (currentComplianceSurvey.getPreparedBySigForReleaseRequestPOE().getId() != null) {
            if (currentComplianceSurvey.getPreparedBySigForReleaseRequestPOE().getSignatureImage() != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(currentComplianceSurvey.getPreparedBySigForReleaseRequestPOE().getSignatureImage()), "image/png");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public StreamedContent getAuthSigForNoticeOfDentionDM() {
        if (currentComplianceSurvey.getAuthSigForNoticeOfDentionDM().getId() != null) {
            if (currentComplianceSurvey.getAuthSigForNoticeOfDentionDM().getSignatureImage() != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(currentComplianceSurvey.getAuthSigForNoticeOfDentionDM().getSignatureImage()), "image/png");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public StreamedContent getApprovedBySigForReleaseRequestPOE() {
        if (currentComplianceSurvey.getApprovedBySigForReleaseRequestPOE().getId() != null) {
            if (currentComplianceSurvey.getApprovedBySigForReleaseRequestPOE().getSignatureImage() != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(currentComplianceSurvey.getApprovedBySigForReleaseRequestPOE().getSignatureImage()), "image/png");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void updateAuthDetentionRequestPOE() {

        if (currentComplianceSurvey.getAuthSigForDetentionRequestPOE().getId() == null) {
            currentComplianceSurvey.setAuthSigDateForDetentionRequestPOE(new Date());
            currentComplianceSurvey.setAuthSigForDetentionRequestPOE(getUser().getEmployee().getSignature());
        } else {
            currentComplianceSurvey.setAuthSigDateForDetentionRequestPOE(null);
            currentComplianceSurvey.setAuthSigForDetentionRequestPOE(null);
        }

    }

    public void updateInspectorSigForSampleRequestPOE() {

        if (currentComplianceSurvey.getInspectorSigForSampleRequestPOE().getId() == null) {
            currentComplianceSurvey.setInspectorSigDateForSampleRequestPOE(new Date());
            currentComplianceSurvey.setInspectorSigForSampleRequestPOE(getUser().getEmployee().getSignature());
        } else {
            currentComplianceSurvey.setInspectorSigDateForSampleRequestPOE(null);
            currentComplianceSurvey.setInspectorSigForSampleRequestPOE(null);
        }

    }

    public void updatePreparedBySigForReleaseRequestPOE() {

        if (currentComplianceSurvey.getPreparedBySigForReleaseRequestPOE().getId() == null) {
            currentComplianceSurvey.setPreparedBySigDateForReleaseRequestPOE(new Date());
            currentComplianceSurvey.setPreparedBySigForReleaseRequestPOE(getUser().getEmployee().getSignature());
            currentComplianceSurvey.setPreparedByEmployeeForReleaseRequestPOE(getUser().getEmployee());
        } else {
            currentComplianceSurvey.setPreparedBySigDateForReleaseRequestPOE(null);
            currentComplianceSurvey.setPreparedBySigForReleaseRequestPOE(null);
            currentComplianceSurvey.setPreparedByEmployeeForReleaseRequestPOE(null);
        }

    }

    public void updateAuthSigForNoticeOfDentionDM() {

        if (currentComplianceSurvey.getAuthSigForNoticeOfDentionDM().getId() == null) {
            currentComplianceSurvey.setAuthSigDateForNoticeOfDentionDM(new Date());
            currentComplianceSurvey.setAuthSigForNoticeOfDentionDM(getUser().getEmployee().getSignature());
        } else {
            currentComplianceSurvey.setAuthSigDateForNoticeOfDentionDM(null);
            currentComplianceSurvey.setAuthSigForNoticeOfDentionDM(null);
        }

    }

    public void updateApprovedBySigForReleaseRequestPOE() {

        if (currentComplianceSurvey.getApprovedBySigForReleaseRequestPOE().getId() == null) {
            currentComplianceSurvey.setApprovedBySigDateForReleaseRequestPOE(new Date());
            currentComplianceSurvey.setApprovedBySigForReleaseRequestPOE(getUser().getEmployee().getSignature());
            currentComplianceSurvey.setApprovedByEmployeeForReleaseRequestPOE(getUser().getEmployee());
        } else {
            currentComplianceSurvey.setApprovedBySigDateForReleaseRequestPOE(null);
            currentComplianceSurvey.setApprovedBySigForReleaseRequestPOE(null);
            currentComplianceSurvey.setApprovedByEmployeeForReleaseRequestPOE(null);
        }

    }

    public ComplianceDailyReport getCurrentComplianceDailyReport() {
        return currentComplianceDailyReport;
    }

    public void setCurrentComplianceDailyReport(ComplianceDailyReport currentComplianceDailyReport) {
        this.currentComplianceDailyReport = currentComplianceDailyReport;
    }

    public List<ComplianceSurvey> getComplianceSurveys() {
        if (complianceSurveys == null) {
            doSurveySearch();
        }
        return complianceSurveys;
    }

    public List<Complaint> getComplaints() {

        if (complaints == null) {
            complaints = Complaint.findAllComplaints(getEntityManager1());
        }

        return complaints;
    }

    public String getDialogMessage() {
        return dialogMessage;
    }

    public void setDialogMessage(String dialogMessage) {
        this.dialogMessage = dialogMessage;
    }

    public String getDialogMessageHeader() {
        return dialogMessageHeader;
    }

    public void setDialogMessageHeader(String dialogMessageHeader) {
        this.dialogMessageHeader = dialogMessageHeader;
    }

    public String getDialogMessageSeverity() {
        return dialogMessageSeverity;
    }

    public void setDialogMessageSeverity(String dialogMessageSeverity) {
        this.dialogMessageSeverity = dialogMessageSeverity;
    }

    public CompanyRegistration getCurrentCompanyRegistration() {
        if (currentCompanyRegistration == null) {
            currentCompanyRegistration = new CompanyRegistration();
        }
        return currentCompanyRegistration;
    }

    private EntityManagerFactory getEMF1() {
        return EMF1;
    }

    // tk put in business entity utils?
    public EntityManager getEntityManager1() {

        entityManager1 = getEMF1().createEntityManager();

        return entityManager1;
    }

    public ProductInspection getCurrentProductInspection() {
        if (currentProductInspection == null) {
            currentProductInspection = new ProductInspection();
        }
        return currentProductInspection;
    }

    public void setCurrentProductInspection(ProductInspection currentProductInspection) {

        this.currentProductInspection = currentProductInspection;
    }

    public List<String> completeManufacturerName(String query) {
        try {
            EntityManager em = getEntityManager1();

            List<String> names = new ArrayList<>();
            List<Manufacturer> manufacturers = Manufacturer.findManufacturersBySearchPattern(em, query);
            for (Manufacturer manufacturer : manufacturers) {
                names.add(manufacturer.getName());
            }

            return names;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<String> completeDistributorName(String query) {
        try {
            EntityManager em = getEntityManager1();

            List<String> names = new ArrayList<>();
            List<Distributor> distributors = Distributor.findDistributorsBySearchPattern(em, query);
            for (Distributor distributor : distributors) {
                names.add(distributor.getName());
            }

            return names;

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

//    public List<Contact> completeRetailOutletRepresentative(String query) {
//        ArrayList<Contact> contactsFound = new ArrayList<>();
//
//        for (Contact contact : currentComplianceSurvey.getRetailOutlet().getContacts()) {
//            if (contact.getFirstName().toUpperCase().contains(query.trim().toUpperCase())
//                    || contact.getLastName().toUpperCase().contains(query.trim().toUpperCase())) {
//                contactsFound.add(contact);
//            }
//        }
//
//        return contactsFound;
//    }
    // Consignee update tk can remove this similar methods like it
    public void updateDocumentInspectionConsignee() {

    }

    public void updateComplianceSurveyConsigneeRepresentative() {

    }

    public void editComplianceSurveyConsignee() {

    }

    public void editDocumentInspectionConsignee() {
    }

    public void updateComplianceSurveyBroker() {
    }

    public void updateComplianceSurveyBroker(EntityManager em) {
    }

    public void updateComplianceSurveyBrokerRepresentative() {
    }

    public List<String> completeComplianceSurveyBrokerRepresentativeName(String query) {
        ArrayList<String> contactsFound = new ArrayList<>();

        return contactsFound;

    }

    public void editComplianceSurveyBroker() {
    }

    public void updateComplianceSurveyRetailOutlet() {
    }

    public void updateProductManufacturerForProductInsp() {
    }

    public void updateProductDistributorForProductInsp() {
    }

    public void updateComplianceSurveyRetailOutlet(EntityManager em) {
    }

    public void updateComplianceSurveyRetailOutletRepresentative() {
    }

    // tk replace this and others like it with clientManager.completeActiveClient()
    public List<String> completeComplianceSurveyRetailOutletRepresentativeName(String query) {
        ArrayList<String> contactsFound = new ArrayList<>();

        return contactsFound;
    }

    public void editComplianceSurveyRetailOutlet() {
    }

    public void updateSurvey() {
        getCurrentComplianceSurvey().setIsDirty(true);
    }

    public void updateComplaint() {
        getCurrentComplaint().setIsDirty(true);
    }

    public void updateFactoryInspection() {
        getCurrentFactoryInspection().setIsDirty(true);
    }

    public void updateEntryDocumentInspection() {
        getCurrentComplianceSurvey().getEntryDocumentInspection().setIsDirty(true);

        updateSurvey();
    }

//    public void updateContainerNumber() {
//        // create/save shipping containers if needed
//        // save current list of containers for later use       
//        List<ShippingContainer> currentShippingContainers = new ArrayList<>();
//        for (ShippingContainer shippingContainer : getCurrentComplianceSurvey().getEntryDocumentInspection().getShippingContainers()) {
//            currentShippingContainers.add(shippingContainer);
//        }
//
//        getCurrentComplianceSurvey().getEntryDocumentInspection().getShippingContainers().clear();
//
//        List<String> numList = getCurrentComplianceSurvey().getEntryDocumentInspection().getContainerNumberList();
//        for (String containerNum : numList) {
//            if (!containerNum.trim().equals("")) {
//                ShippingContainer sc = getCurrentShippingContainerByNumber(currentShippingContainers, containerNum);
//                if (sc == null) {
//                    getCurrentComplianceSurvey().getEntryDocumentInspection().getShippingContainers().add(new ShippingContainer(containerNum));
//                } else {
//                    getCurrentComplianceSurvey().getEntryDocumentInspection().getShippingContainers().add(sc);
//                }
//            }
//        }
//
//        //setDirty(true);
//    }
    public ShippingContainer getCurrentShippingContainerByNumber(List<ShippingContainer> shippingContainers, String number) {
        for (ShippingContainer shippingContainer : shippingContainers) {
            if (shippingContainer.getNumber().trim().equals(number.trim())) {
                return shippingContainer;
            }
        }

        return null;
    }

    public void updateDateOfDetention() {
        getCurrentComplianceSurvey().setDateOfDetention(new Date());

        updateSurvey();
    }

    public void updateDailyReportStartDate() {
        currentComplianceDailyReport.setEndOfPeriod(currentComplianceDailyReport.getStartOfPeriod());
        //endOfPeriod = startOfPeriod;
        //setDirty(true);
    }

    public void updateCountryOfConsignment() {

    }

    public void updateCompanyTypes() {
        if (!currentComplianceSurvey.getOtherCompanyTypes()) {
            currentComplianceSurvey.setCompanyTypes("");
        }
        //setDirty(true);
    }

    public void createNewProductInspection() {
        currentProductInspection = new ProductInspection();
        currentProductInspection.setQuantity(0);
        currentProductInspection.setSampleSize(0);

        setEdit(false);

        openProductInspectionDialog();
    }

    public void createNewComplaintProductInspection() {
        currentProductInspection = new ProductInspection();
        currentProductInspection.setQuantity(0);
        currentProductInspection.setSampleSize(0);

        setEdit(false);

        openComplaintProductInspectionDialog();
    }

    public ComplianceSurvey getCurrentComplianceSurvey() {

        return currentComplianceSurvey;
    }

    public void setCurrentComplianceSurvey(ComplianceSurvey currentComplianceSurvey) {
        this.currentComplianceSurvey = currentComplianceSurvey;
    }

    public List<Address> completeManufacturerAddress(String query) {
        List<Address> addresses = new ArrayList<>();

        try {

            for (Address address : getCurrentFactoryInspection().getManufacturer().getAddresses()) {
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

    public void updateManufacturer() {

        currentFactoryInspection.setAddress(new Address());
        currentFactoryInspection.setFactoryRepresentative(new Contact());

        updateFactoryInspection();
    }

    public Complaint getCurrentComplaint() {
        return currentComplaint;
    }

    public void setCurrentComplaint(Complaint currentComplaint) {
        this.currentComplaint = currentComplaint;
    }

    public void createNewComplianceSurvey() {

        currentComplianceSurvey = new ComplianceSurvey();
        currentComplianceSurvey.setSurveyLocationType("Commercial Marketplace");
        currentComplianceSurvey.setSurveyType("Commercial Marketplace");
        currentComplianceSurvey.setDateOfSurvey(new Date());
        currentComplianceSurvey.setInspector(getUser().getEmployee());

        editComplianceSurvey();

        openSurveysBrowser();
    }

    public void createNewComplaint() {

        currentComplaint = new Complaint();
        currentComplaint.setDateReceived(new Date());
        currentComplaint.setEnteredBy(getUser().getEmployee());

        editComplaint();

        openComplaintsBrowser();
    }

    public void createNewDocumentInspection() {

        currentDocumentInspection = new DocumentInspection();

        currentDocumentInspection.setName(" ");

        currentDocumentInspection.setDateOfInspection(new Date());
        if (getUser() != null) {
            currentDocumentInspection.setInspector(getUser().getEmployee());
        }

    }

    public void saveComplianceSurvey() {
        EntityManager em = getEntityManager1();

        try {

            // Ensure inspector is not null
            Employee inspector = Employee.findEmployeeByName(em, currentComplianceSurvey.getInspector().getName());
            if (inspector != null) {
                currentComplianceSurvey.setInspector(inspector);
            } else {
                currentComplianceSurvey.setInspector(Employee.findDefaultEmployee(em, "--", "--", true));
            }

            // Validate fields required for port of entry detention if one was issued
            // NB: This should be in validation code.
            if (currentComplianceSurvey.getRequestForDetentionIssuedForPortOfEntry()) {
                if (!validatePortOfEntryDetentionData(em)) {
                    return;
                }
            }

            if (getCurrentComplianceSurvey().getIsDirty()) {
                currentComplianceSurvey.setDateEdited(new Date());
                currentComplianceSurvey.setEditedBy(getUser().getEmployee());
            }
            //em.getTransaction().begin();

            // Now save survey            
            //Long id = BusinessEntityUtils.saveBusinessEntity(em, currentComplianceSurvey);
            //em.getTransaction().commit();
            ReturnMessage message = currentComplianceSurvey.save(em);

            if (!message.isSuccess()) {
                PrimeFacesUtils.addMessage("Save Error!",
                        "An error occured while saving this survey",
                        FacesMessage.SEVERITY_ERROR);
            } else {
                //isNewComplianceSurvey = false;
                currentComplianceSurvey.setIsDirty(false);
                PrimeFacesUtils.addMessage("Survey Saved!",
                        "This survey was saved",
                        FacesMessage.SEVERITY_INFO);
            }

        } catch (Exception e) {

            System.out.println(e);
        }
    }

    public void saveFactoryInspection() {
        EntityManager em = getEntityManager1();

        try {

            ReturnMessage message = currentFactoryInspection.save(em);

            if (!message.isSuccess()) {
                PrimeFacesUtils.addMessage("Save Error!",
                        "An error occured while saving this factory inspection",
                        FacesMessage.SEVERITY_ERROR);
            } else {

                currentFactoryInspection.setIsDirty(false);
                PrimeFacesUtils.addMessage("Factory inspection Saved!",
                        "This factory inspection was saved",
                        FacesMessage.SEVERITY_INFO);
            }

        } catch (Exception e) {

            System.out.println(e);
        }
    }

    public void saveComplaint() {
        EntityManager em = getEntityManager1();

        try {

            if (getCurrentComplaint().getEnteredBy().getId() == null) {
                getCurrentComplaint().setEnteredBy(getUser().getEmployee());
            }

            // Now save complaint  
            ReturnMessage message = getCurrentComplaint().save(em);

            if (!message.isSuccess()) {
                PrimeFacesUtils.addMessage("Save Error!",
                        "An error occured while saving this complaint",
                        FacesMessage.SEVERITY_ERROR);
            } else {

                getCurrentComplaint().setIsDirty(false);
                PrimeFacesUtils.addMessage("Complaint Saved!",
                        "This complaint was saved",
                        FacesMessage.SEVERITY_INFO);
            }

        } catch (Exception e) {

            System.out.println(e);
        }
    }

    public Boolean validatePortOfEntryDetentionData(EntityManager em) {
        if (Job.findJobByJobNumber(em, currentComplianceSurvey.getJobNumber()) == null) {
            PrimeFacesUtils.addMessage("Job Number Required!",
                    "A valid job number is required if a detention request is issued.",
                    FacesMessage.SEVERITY_ERROR);
            return false;
        }
        if (currentComplianceSurvey.getBroker().getName().trim().equals("")) {
            PrimeFacesUtils.addMessage("Broker Required",
                    "The broker name is required if a detention request is issued.",
                    FacesMessage.SEVERITY_ERROR);
            return false;
        }

        if (currentComplianceSurvey.getDateOfDetention() == null) {
            PrimeFacesUtils.addMessage("Date of Detention Required",
                    "The date of the detention is required if a detention request is issued.",
                    FacesMessage.SEVERITY_ERROR);
            return false;
        }
        if (currentComplianceSurvey.getReasonForDetention().trim().equals("")) {
            PrimeFacesUtils.addMessage("Reason for Detention Required",
                    "The reason for the detention is required if a detention request is issued.",
                    FacesMessage.SEVERITY_ERROR);
            return false;
        }

        return true;
    }

    public void closeComplianceSurvey() {
        PrimeFacesUtils.closeDialog(null);
    }

    public void closeDialog() {
        PrimeFacesUtils.closeDialog(null);
    }

    public void closeDocumentInspection() {
        promptToSaveIfRequired();
    }

    public void cancelProductInspection() {
        PrimeFacesUtils.closeDialog(null);
    }

    public Boolean getIsNewProductInspection() {
        return getCurrentProductInspection().getId() == null && !getEdit();
    }

    public void okProductInspection() {
        try {

            if (getIsNewProductInspection()) {
                currentComplianceSurvey.getProductInspections().add(currentProductInspection);
            }

            currentProductInspection.setInspector(getUser().getEmployee());
            currentComplianceSurvey.setInspector(getUser().getEmployee());

            PrimeFacesUtils.closeDialog(null);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void okComplaintProductInspection() {
        try {

            if (getIsNewProductInspection()) {
                currentComplaint.getProductInspections().add(currentProductInspection);
            }

            PrimeFacesUtils.closeDialog(null);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void removeProductInspection(ActionEvent event) {

        currentComplianceSurvey.getProductInspections().remove(currentProductInspection);
        currentProductInspection = new ProductInspection();

        currentComplianceSurvey.setIsDirty(true);

    }

    public void removeComplaintProductInspection(ActionEvent event) {

        currentComplaint.getProductInspections().remove(currentProductInspection);
        currentProductInspection = new ProductInspection();

        currentComplaint.setIsDirty(true);

    }

    public String getLatestAlert() {
        return "*********** TOYS R US BABY STROLLER MODEL # 3213 **********";
    }

    private void promptToSaveIfRequired() {
        // tk impl 
        System.out.println("impl promptToSaveIfRequired");
    }

    public List<SelectItem> getProductStatus() {

        return getStringListAsSelectItems(getEntityManager1(), "productStatusList");
    }

    public List<SelectItem> getShippingContainerDetainPercentages() {
        return getStringListAsSelectItems(getEntityManager1(), "shippingContainerPercentageList");
    }

    public List<SelectItem> getPortsOfEntry() {

        return getStringListAsSelectItems(getEntityManager1(), "compliancePortsOfEntry");
    }

    public List<SelectItem> getInspectionPoints() {

        return getStringListAsSelectItems(getEntityManager1(), "complianceSurveyMiscellaneousInspectionPointList");
    }

    public List getDocumentInspectionActions() {

        return getStringListAsSelectItems(getEntityManager1(), "portOfEntryDocumentStampList");
    }

    public List<SelectItem> getSurveyTypes() {

        return getStringListAsSelectItems(getEntityManager1(), "complianceSurveyTypes");
    }

    public List getSampleSources() {

        return getStringListAsSelectItems(getEntityManager1(), "complianceSampleSources");
    }

    public void updateComplianceSurveyInspector() {

        System.out.println("impl ...");
    }

    public void updateDocumentInspectionInspector() {

        System.out.println("impl updateDocumentInspectionInspector");
    }

    public String getSurveyFormComponentsToUpdate() {
        return "compliance_survey_growl";
    }

    public String getDateSearchField() {
        return dateSearchField;
    }

    public void setDateSearchField(String dateSearchField) {
        this.dateSearchField = dateSearchField;
    }

    public String getDateSearchPeriod() {
        return dateSearchPeriod;
    }

    public void setDateSearchPeriod(String dateSearchPeriod) {
        this.dateSearchPeriod = dateSearchPeriod;
    }

    public Date getReportEndDate() {
        return reportEndDate;
    }

    public void setReportEndDate(Date reportEndDate) {
        this.reportEndDate = reportEndDate;
    }

    public String getReportPeriod() {
        return reportPeriod;
    }

    public void setReportPeriod(String reportPeriod) {
        this.reportPeriod = reportPeriod;
    }

    public String getReportSearchText() {
        return reportSearchText;
    }

    public void setReportSearchText(String reportSearchText) {
        this.reportSearchText = reportSearchText;
    }

    public Date getReportStartDate() {
        return reportStartDate;
    }

    public void setReportStartDate(Date reportStartDate) {
        this.reportStartDate = reportStartDate;
    }

    public String getSurveySearchText() {
        return surveySearchText;
    }

    public void setSurveySearchText(String surveySearchText) {
        this.surveySearchText = surveySearchText;
    }

    public String getStandardSearchText() {
        return standardSearchText;
    }

    public void setStandardSearchText(String standardSearchText) {
        this.standardSearchText = standardSearchText;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public void doDefaultSearch() {

        switch (getSystemManager().getDashboard().getSelectedTabId()) {
            case "Survey Browser":
                doSurveySearch();
                break;
            default:
                break;
        }

    }

    public void doSurveySearch() {
        complianceSurveys = ComplianceSurvey.findComplianceSurveysByDateSearchField(getEntityManager1(),
                getUser(),
                dateSearchField,
                "General",
                surveySearchText,
                getDatePeriod().getStartDate(),
                getDatePeriod().getEndDate(),
                false);

        openSurveysBrowser();
    }

    public List<String> completeSearchText(String query) {
        List<String> suggestions = new ArrayList<>();

        // NB: This is only an example implementation
        // based on the current code based, the following code is never called
        if (searchType.equals("?")) {
            // add suggestion strings to the suggestions list
        }

        return suggestions;
    }

    public void handleProductPhotoFileUpload(FileUploadEvent event) {
        FileOutputStream fout;
        UploadedFile upLoadedFile = event.getFile();
        String baseURL = "\\\\bosprintsrv\\c$\\uploads\\images"; // ".\\uploads\\images\\" +  // tk to be made system option
        String upLoadedFileName = getUser().getId() + "_"
                + new Date().getTime() + "_"
                + upLoadedFile.getFileName();

        String imageURL = baseURL + "\\" + upLoadedFileName;

        try {
            fout = new FileOutputStream(imageURL);
            fout.write(upLoadedFile.getContent());
            fout.close();

            getCurrentProductInspection().setImageURL(upLoadedFileName);
            //setDirty(true);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void editComplianceSurvey() {
        openComplianceSurvey();
    }

    public void editComplaint() {
        PrimeFacesUtils.openDialog(null, "/compliance/complaintDialog", true, true, true, true, 650, 800);
    }

    public Boolean getComplianceSurveyIsValid() {
//        if (getCurrentComplianceSurvey().getId() != null) {
//            return true;
//        } else if (dirty) {
//            return true;
//        }

        return false;
    }

    public Boolean getProductInspectionImageIsValid() {
        return !getCurrentProductInspection().getImageURL().isEmpty();
    }

    // start long process bar related methods
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

    public void onLongProcessComplete() {
        longProcessProgress = 0;
    }

    public void setLongProcessProgress(Integer longProcessProgress) {
        this.longProcessProgress = longProcessProgress;
    }
    // end long process bar related methods

    // tk to download actual file
    public StreamedContent getCurrentProductInspectionImageDownload() {
        StreamedContent streamedFile = null;

        // tk test gen report
        HashMap parameters = new HashMap();

        Connection con = BusinessEntityUtils.establishConnection("com.mysql.jdbc.Driver",
                "jdbc:mysql://boshrmapp:3306/jmtstest",
                "root", // make system option
                "bsj0001");  // make sys
        if (con != null) {
            System.out.println("connected!");
            String reportFileURL = "//bosprintsrv/c$/uploads/templates/DetentionRequest.jasper";
            try {
                parameters.put("formId", 178153L);
                JasperPrint print = JasperFillManager.fillReport(reportFileURL, parameters, con);

            } catch (JRException ex) {
                System.out.println(ex);
            }

        } else {
            System.out.println("not connected!");
        }

        String baseURL = "C:\\glassfishv3\\images\\11153_1359128328029_print-file.png";

        try {

            FileInputStream stream = new FileInputStream(baseURL);
            streamedFile = new DefaultStreamedContent(stream, "image/png", "downloaded.png");

        } catch (FileNotFoundException ex) {
            System.out.println(ex);
        }

        return streamedFile;
    }

    public StreamedContent getDetentionRequestFile() {

        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();
//
//        updateComplianceSurvey(em);
//        // set parameters
//        parameters.put("formId", currentComplianceSurvey.getId().longValue());
//
//        // broker detail
//        //Client broker = jm.getClientByName(em, currentComplianceSurvey.getBroker().getName());
//        //Contact brokerRep = jm.getContactByName(em, username, username);
//        parameters.put("brokerDetail", currentComplianceSurvey.getBroker().getName() + "\n"
//                + currentComplianceSurvey.getBrokerRepresentative().getFirstName() + " "
//                + currentComplianceSurvey.getBrokerRepresentative().getLastName() + "\n"
//                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine1() + "\n"
//                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine2() + "\n"
//                + currentComplianceSurvey.getBroker().getBillingAddress().getCity() + "\n"
//                + currentComplianceSurvey.getBroker().getBillingAddress().getStateOrProvince());
//
//        //Consignee detail
//        //Client consignee = jm.getClientByName(em, currentComplianceSurvey.getConsignee().getName());
//        parameters.put("consigneeDetail", currentComplianceSurvey.getConsignee().getName() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine1() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine2() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getCity() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getStateOrProvince() + "\n"
//                + BusinessEntityUtils.getContactTelAndFax(currentComplianceSurvey.getConsignee().getMainContact()));
//
//        parameters.put("products", getComplianceSurveyProductNames());
//        parameters.put("quantity", getComplianceSurveyProductQuantitiesAndUnits());
//        parameters.put("numberOfSamplesTaken", getComplianceSurveyProductTotalSampleSize());

        return getComplianceSurveyFormPDFFile(
                em,
                "portOfEntryDetentionRequestForm",
                "detention_request.pdf",
                parameters,
                "PORT_OF_ENTRY_DETENTION");
    }

    public StreamedContent getReleaseRequestForPortOfEntryFile() {

        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();

//        updateComplianceSurvey(em);
//
//        // specified release location               
//        parameters.put("specifiedReleaseLocation", currentComplianceSurvey.getSpecifiedReleaseLocation().getName() + "\n"
//                + currentComplianceSurvey.getSpecifiedReleaseLocation().getAddressLine1() + "\n"
//                + currentComplianceSurvey.getSpecifiedReleaseLocation().getAddressLine2() + "\n"
//                + currentComplianceSurvey.getSpecifiedReleaseLocation().getCity() + "\n"
//                + currentComplianceSurvey.getSpecifiedReleaseLocation().getStateOrProvince());
//
//        // consignee
//        // Client consignee = jm.getClientByName(em, currentComplianceSurvey.getConsignee().getName());
//        parameters.put("consigneeDetail", currentComplianceSurvey.getConsignee().getName() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine1() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine2() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getCity() + "\n"
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getStateOrProvince());
//
//        parameters.put("products", getComplianceSurveySampledProductNamesQuantitiesAndUnits());
//        parameters.put("numberOfSamplesTaken", getComplianceSurveyProductTotalSampleSize());
        return getComplianceSurveyFormPDFFile(
                em,
                "portOfEntryReleaseRequestForm",
                "release_request.pdf",
                parameters,
                "PORT_OF_ENTRY_DETENTION");

    }

    public StreamedContent getSampleRequestFile() {

        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();

        updateComplianceSurvey(em);

        // broker
        parameters.put("formId", currentComplianceSurvey.getId().longValue());
        parameters.put("brokerDetail", currentComplianceSurvey.getBroker().getName() + "\n"
                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine1() + "\n"
                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine2() + "\n"
                + BusinessEntityUtils.getContactTelAndFax(currentComplianceSurvey.getBroker().getMainContact()));

        // consignee
        parameters.put("consigneeDetail", currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine1() + ", "
                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine2() + ", "
                + currentComplianceSurvey.getConsignee().getBillingAddress().getCity() + ", "
                + currentComplianceSurvey.getConsignee().getBillingAddress().getStateOrProvince());

        // consignee contact person
        parameters.put("consigneeContactPerson", BusinessEntityUtils.getContactFullName(currentComplianceSurvey.getConsigneeRepresentative()));

        parameters.put("consigneeTelFaxEmail", BusinessEntityUtils.getMainTelFaxEmail(currentComplianceSurvey.getConsignee().getMainContact()));
        parameters.put("products", getComplianceSurveyProductNames());
        parameters.put("quantity", getComplianceSurveyProductQuantitiesAndUnits());
        parameters.put("numberOfSamplesTaken", getComplianceSurveyProductTotalSampleSize());

        // sample disposal
        if (currentComplianceSurvey.getSamplesToBeCollected()) {
            parameters.put("samplesToBeCollected", "\u2713"); // \u2713 is unicode for tick
        } else {
            parameters.put("samplesToBeCollected", "");
        }
        if (currentComplianceSurvey.getSamplesToBeDisposed()) {
            parameters.put("samplesToBeDisposed", "\u2713");
        } else {
            parameters.put("samplesToBeDisposed", "");
        }

        return getComplianceSurveyFormPDFFile(
                em,
                "portOfEntryDetentionSampleRequestForm",
                "sample_request.pdf",
                parameters,
                "PORT_OF_ENTRY_DETENTION");
    }

    public StreamedContent getNoticeOfReleaseFromDetentionFile() {
        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();

        updateComplianceSurvey(em);

        // full release
        if (currentComplianceSurvey.getFullRelease()) {
            parameters.put("fullRelease", "\u2713");
        } else {
            parameters.put("fullRelease", "");
        }

        // retailer, distributor, other?
        if (currentComplianceSurvey.getRetailer()) {
            parameters.put("retailer", "\u2713");
        } else {
            parameters.put("retailer", "");
        }
        if (currentComplianceSurvey.getDistributor()) {
            parameters.put("distributor", "\u2713");
        } else {
            parameters.put("distributor", "");
        }
        if (currentComplianceSurvey.getOtherCompanyTypes()) {
            parameters.put("otherCompanyTypes", "\u2713");
            parameters.put("companyTypes", currentComplianceSurvey.getCompanyTypes());
        } else {
            parameters.put("otherCompanyTypes", "");
            currentComplianceSurvey.setCompanyTypes("");
            parameters.put("companyTypes", currentComplianceSurvey.getCompanyTypes());
        }

        // broker
        parameters.put("formId", currentComplianceSurvey.getId().longValue());
        parameters.put("brokerDetail", currentComplianceSurvey.getBroker().getName() + "\n"
                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine1() + "\n"
                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine2() + "\n"
                + BusinessEntityUtils.getContactTelAndFax(currentComplianceSurvey.getBroker().getMainContact()));

        // consignee
        parameters.put("consigneeDetail", currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine1() + ", "
                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine2() + ", "
                + currentComplianceSurvey.getConsignee().getBillingAddress().getCity() + ", "
                + currentComplianceSurvey.getConsignee().getBillingAddress().getStateOrProvince());

        // provisional release location 
        parameters.put("specifiedReleaseLocationDomesticMarket", currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getAddressLine1() + ", "
                + currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getAddressLine2() + ", "
                + currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getCity() + ", "
                + currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getStateOrProvince());

        // location of detained products locationOfDetainedProduct 
        parameters.put("locationOfDetainedProduct", currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getAddressLine1() + ", "
                + currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getAddressLine2() + ", "
                + currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getCity() + ", "
                + currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getStateOrProvince());

        // consignee contact person
        parameters.put("consigneeContactPerson", BusinessEntityUtils.getContactFullName(currentComplianceSurvey.getConsigneeRepresentative()));

        // consignee tel/fax/email
        parameters.put("consigneeTelFaxEmail", BusinessEntityUtils.getMainTelFaxEmail(currentComplianceSurvey.getConsignee().getMainContact()));

        parameters.put("products", getComplianceSurveyProductNames());
        parameters.put("productBrandNames", getComplianceSurveyProductBrandNames());
        parameters.put("productBatchCodes", getComplianceSurveyProductBatchCodes());
        parameters.put("quantity", getComplianceSurveyProductQuantitiesAndUnits());
        parameters.put("numberOfSamplesTaken", getComplianceSurveyProductTotalSampleSize());

        return getComplianceSurveyFormPDFFile(
                em,
                "noticeOfReleaseFromDetentionForm",
                "release_notice.pdf",
                parameters,
                "DOMESTIC_MARKET_DETENTION");
    }

    public String getComplianceSurveyProductNames() {
        String names = "";

        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
            if (names.equals("")) {
                names = product.getName();
            } else {
                names = names + ", " + product.getName();
            }
        }

        return names;
    }

    public Boolean samplesTaken() {
        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
            if (product.getSampledForLabelAssessment() || product.getSampledForTesting()) {
                return true;
            }
        }

        return false;
    }

    public String getComplianceSurveyProductBrandNames() {
        String brandNames = "";

        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
            if (brandNames.equals("")) {
                brandNames = product.getBrand();
            } else {
                brandNames = brandNames + ", " + product.getBrand();
            }
        }

        return brandNames;
    }

    public String getComplianceSurveyProductBatchCodes() {
        String batchCodes = "";

        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
            if (batchCodes.equals("")) {
                batchCodes = product.getBatchCode();
            } else {
                batchCodes = batchCodes + ", " + product.getBatchCode();
            }
        }

        return batchCodes;
    }

    public String getComplianceSurveyProductTotalQuantity() {
        Integer totalQuantity = 0;

        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
            if (product.getQuantity() != null) {
                totalQuantity = totalQuantity + product.getQuantity();
            }
        }

        return totalQuantity.toString();
    }

    public String getComplianceSurveySampledProductNamesQuantitiesAndUnits() {
        String namesQuantitiesAndUnits = "";

        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
            if (product.getSampledForTesting() || product.getSampledForLabelAssessment()) {
                if (namesQuantitiesAndUnits.equals("")) {
                    namesQuantitiesAndUnits = namesQuantitiesAndUnits + product.getContainerSize() + " " + product.getQuantity() + " " + product.getQuantityUnit() + " of " + product.getName();
                } else {
                    namesQuantitiesAndUnits = namesQuantitiesAndUnits + ", " + product.getContainerSize() + " " + product.getQuantity() + " " + product.getQuantityUnit() + " of " + product.getName();
                }
            }
        }

        return namesQuantitiesAndUnits;
    }

    public String getComplianceSurveyProductQuantitiesAndUnits() {
        String quantitiesAndUnits = "";

        for (ProductInspection product : currentComplianceSurvey.getProductInspections()) {
//            if (product.getQuantity() != null) {
            if (quantitiesAndUnits.equals("")) {
                quantitiesAndUnits = quantitiesAndUnits + product.getContainerSize() + " " + product.getQuantity() + " " + product.getQuantityUnit();
            } else {
                quantitiesAndUnits = quantitiesAndUnits + ", " + product.getContainerSize() + " " + product.getQuantity() + " " + product.getQuantityUnit();
            }
//            }
        }

        return quantitiesAndUnits;
    }

    public String getComplianceSurveyProductTotalSampleSize() {
        Integer totalSampleSize = 0;

        for (ProductInspection productInspection : currentComplianceSurvey.getProductInspections()) {
            if (productInspection.getNumProductsSampled() != null) {
                totalSampleSize = totalSampleSize + productInspection.getNumProductsSampled();
            }
        }

        return totalSampleSize.toString();
    }

    // tk use of this may have to be retired.
    public void updateComplianceSurvey(EntityManager em) {
//        if (dirty) {
//            save();
//        }
//        if (currentComplianceSurvey.getId() != null) {
//            currentComplianceSurvey = ComplianceSurvey.findComplianceSurveyById(em, currentComplianceSurvey.getId());
//        }
    }

    public StreamedContent getComplianceSurveyFormPDFFile(
            EntityManager em,
            String form,
            String fileName,
            HashMap parameters,
            String sequentialNumberName) {

        if (currentComplianceSurvey.getId() != null) {
            try {
                Connection con = BusinessEntityUtils.establishConnection(
                        "com.mysql.jdbc.Driver",
                        "jdbc:mysql://boshrmapp:3306/jmtstest", // tk make system option
                        "root", // tk make system option
                        "bsj0001");  // tk make system option
                if (con != null) {
                    StreamedContent streamContent;

                    String reportFileURL = (String) SystemOption.getOptionValueObject(em, form);

                    // make sure is parameter is set for all forms
                    parameters.put("formId", currentComplianceSurvey.getId().longValue());

                    // tk remove and do this when compliance survey is being saved
                    // get and set reference number if it is null
                    if (sequentialNumberName.equals("PORT_OF_ENTRY_DETENTION")) {
                        if (currentComplianceSurvey.getPortOfEntryDetentionNumber() == null) {
                            em.getTransaction().begin();
                            int year = BusinessEntityUtils.getCurrentYear();
                            currentComplianceSurvey. // tk BSJ-D42- to be made option?
                                    setPortOfEntryDetentionNumber("BSJ-D42-" + year + "-"
                                            + BusinessEntityUtils.getFourDigitString(SequenceNumber.findNextSequentialNumberByNameAndByYear(em, sequentialNumberName, year)));
                            //setDirty(dirty);
                            updateComplianceSurvey(em);
                            em.getTransaction().commit();

                        }
                        //parameters.put("referenceNumber", currentComplianceSurvey.getReferenceNumber());
                    } else if (sequentialNumberName.equals("DOMESTIC_MARKET_DETENTION")) {
                        if (currentComplianceSurvey.getDomesticMarketDetentionNumber() == null) {
                            em.getTransaction().begin();
                            int year = BusinessEntityUtils.getCurrentYear();
                            currentComplianceSurvey. // tk BSJ-D42- to be made option?
                                    setDomesticMarketDetentionNumber("BSJ-DM42-" + year + "-"
                                            + BusinessEntityUtils.getFourDigitString(SequenceNumber.findNextSequentialNumberByNameAndByYear(em, sequentialNumberName, year)));
                            //setDirty(dirty);
                            updateComplianceSurvey(em);
                            em.getTransaction().commit();

                        }
                        //parameters.put("referenceNumber", currentComplianceSurvey.getReferenceNumber());
                    }

                    // generate report
                    JasperPrint print = JasperFillManager.fillReport(reportFileURL, parameters, con);

                    byte[] fileBytes = JasperExportManager.exportReportToPdf(print);

                    streamContent = new DefaultStreamedContent(new ByteArrayInputStream(fileBytes), "application/pdf", fileName);
                    setLongProcessProgress(100);

                    return streamContent;
                } else {
                    return null;
                }
            } catch (JRException e) {
                System.out.println(e);
                setLongProcessProgress(100);

                return null;
            }
        } else {
            return null;
        }
    }

    // tk
    public StreamedContent getComplianceDailyReportPDFFile() {

        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();

        try {
            Connection con = BusinessEntityUtils.establishConnection(
                    "com.mysql.jdbc.Driver",
                    "jdbc:mysql://boshrmapp:3306/jmtstest", // tk make system option
                    "root", // tk make system option
                    "bsj0001");  // tk make system option
            if (con != null) {
                StreamedContent streamContent;

                String reportFileURL = (String) SystemOption.getOptionValueObject(em, "complianceDailyReport");

                // make sure is parameter is set for all forms
                parameters.put("team", currentComplianceDailyReport.getTeam());
                parameters.put("startOfPeriod", currentComplianceDailyReport.getStartOfPeriod());
                parameters.put("endOfPeriod", currentComplianceDailyReport.getEndOfPeriod());
                parameters.put("timeOfArrival", currentComplianceDailyReport.getTimeOfArrival());
                parameters.put("timeOfDeparture", currentComplianceDailyReport.getTimeOfDeparture());
                parameters.put("inspectionPoint", "One Stop Shop"); // tk ,make option
                parameters.put("inspectionPoint_2", "Stripping Station"); // tk make option
                parameters.put("location", currentComplianceDailyReport.getLocation());
                parameters.put("teamMembers", currentComplianceDailyReport.getTeamMembers());
                parameters.put("driver", currentComplianceDailyReport.getDriver());
                parameters.put("numOfInspectorsOnVisit", currentComplianceDailyReport.getNumOfInspectorsOnVisit());

                // generate report
                JasperPrint print = JasperFillManager.fillReport(reportFileURL, parameters, con);

                byte[] fileBytes = JasperExportManager.exportReportToPdf(print);

                streamContent = new DefaultStreamedContent(new ByteArrayInputStream(fileBytes), "application/pdf", "daily_report.pdf");
                setLongProcessProgress(100);

                return streamContent;
            } else {
                return null;
            }

        } catch (JRException e) {
            System.out.println(e);
            setLongProcessProgress(100);

            return null;
        }
    }

    public StreamedContent getNoticeOfDetentionFile() {
        EntityManager em = getEntityManager1();
        HashMap parameters = new HashMap();

//        updateComplianceSurvey(em);
//
//        // retailer, distributor, other?
//        if (currentComplianceSurvey.getRetailer()) {
//            parameters.put("retailer", "\u2713");
//        } else {
//            parameters.put("retailer", "");
//        }
//        if (currentComplianceSurvey.getDistributor()) {
//            parameters.put("distributor", "\u2713");
//        } else {
//            parameters.put("distributor", "");
//        }
//        if (currentComplianceSurvey.getOtherCompanyTypes()) {
//            parameters.put("otherCompanyTypes", "\u2713");
//            parameters.put("companyTypes", currentComplianceSurvey.getCompanyTypes());
//        } else {
//            parameters.put("otherCompanyTypes", "");
//            currentComplianceSurvey.setCompanyTypes("");
//            parameters.put("companyTypes", currentComplianceSurvey.getCompanyTypes());
//        }
//
//        // broker
//        parameters.put("formId", currentComplianceSurvey.getId().longValue());
//        parameters.put("brokerDetail", currentComplianceSurvey.getBroker().getName() + "\n"
//                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine1() + "\n"
//                + currentComplianceSurvey.getBroker().getBillingAddress().getAddressLine2() + "\n"
//                + BusinessEntityUtils.getContactTelAndFax(currentComplianceSurvey.getBroker().getMainContact()));
//
//        // consignee
//        parameters.put("consigneeDetail", currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine1() + ", "
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getAddressLine2() + ", "
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getCity() + ", "
//                + currentComplianceSurvey.getConsignee().getBillingAddress().getStateOrProvince());
//
//        // provisional release location 
//        parameters.put("specifiedReleaseLocationDomesticMarket", currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getAddressLine1() + ", "
//                + currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getAddressLine2() + ", "
//                + currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getCity() + ", "
//                + currentComplianceSurvey.getSpecifiedReleaseLocationDomesticMarket().getStateOrProvince());
//
//        // location of detained products locationOfDetainedProduct 
//        parameters.put("locationOfDetainedProduct", currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getAddressLine1() + ", "
//                + currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getAddressLine2() + ", "
//                + currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getCity() + ", "
//                + currentComplianceSurvey.getLocationOfDetainedProductDomesticMarket().getStateOrProvince());
//
//        // consignee tel/fax/email
//        parameters.put("consigneeTelFaxEmail", BusinessEntityUtils.getMainTelFaxEmail(currentComplianceSurvey.getConsignee().getMainContact()));
//
//        parameters.put("products", getComplianceSurveyProductNames());
//        parameters.put("productBrandNames", getComplianceSurveyProductBrandNames());
//        parameters.put("productBatchCodes", getComplianceSurveyProductBatchCodes());
//        parameters.put("quantity", getComplianceSurveyProductQuantitiesAndUnits());
//        parameters.put("numberOfSamplesTaken", getComplianceSurveyProductTotalSampleSize());
//
//        if (samplesTaken()) {
//            parameters.put("samplesTaken", "\u2713");
//        } else {
//            parameters.put("samplesTaken", "");
//        }
        return getComplianceSurveyFormPDFFile(
                em,
                "noticeOfDetentionForm",
                "detention_notice.pdf",
                parameters,
                "DOMESTIC_MARKET_DETENTION");
    }

    public User getUser() {
        return getSystemManager().getAuthentication().getUser();
    }

    private void initMainTabView() {
        if (getUser().getModules().getComplianceModule()) {
//            getSystemManager().getMainTabView().openTab("Survey Browser");
//            getSystemManager().getMainTabView().openTab("Standard Browser");
//            getSystemManager().getMainTabView().openTab("Complaint Browser");
//            getSystemManager().getMainTabView().openTab("Market Products");
//            getSystemManager().getMainTabView().openTab("Manufacturers");
            getSystemManager().getMainTabView().openTab("Factory Inspections");
        }

    }

    private void initDashboard() {

        if (getUser().getModules().getComplianceModule()) {
            getSystemManager().getDashboard().openTab("Standards Compliance");
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

    public List<DocumentStandard> completeActiveDocumentStandard(String query) {
        try {
            return DocumentStandard.findActiveDocumentStandardsByAnyPartOfNameOrNumber(getEntityManager1(), query);

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public List<Category> completeActiveCategory(String query) {
        try {
            return Category.findActiveCategoriesByAnyPartOfNameAndType(
                    getEntityManager1(),
                    "Product",
                    query);

        } catch (Exception e) {
            System.out.println(e);

            return new ArrayList<>();
        }
    }

    public void createNewMarketProduct() {
        currentMarketProduct = new MarketProduct();

        openMarketProductDialog();

        openMarketProductBrowser();
    }

    public void openMarketProductDialog() {
        PrimeFacesUtils.openDialog(null, "/compliance/marketProductDialog", true, true, true, true, 650, 800);
    }

    public void openMarketProductBrowser() {

        getSystemManager().getMainTabView().openTab("Market Products");
    }

    public void doMarketProductSearch() {

        if (getIsActiveMarketProductsOnly()) {
            marketProducts = MarketProduct.findActiveMarketProductsByAnyPartOfNameOrDescription(getEntityManager1(), marketProductSearchText);
        } else {
            marketProducts = MarketProduct.findMarketProductsByAnyPartOfNameOrDescription(getEntityManager1(), marketProductSearchText);
        }

    }

    public DocumentStandard getCurrentDocumentStandard() {
        return currentDocumentStandard;
    }

    public void setCurrentDocumentStandard(DocumentStandard currentDocumentStandard) {
        this.currentDocumentStandard = currentDocumentStandard;
    }

    public void createNewDocumentStandard() {
        currentDocumentStandard = new DocumentStandard();

        openDocumentStandardDialog();

        openStandardsBrowser();
    }

    public List<DocumentStandard> getDocumentStandards() {

        if (documentStandards == null) {
            documentStandards = DocumentStandard.findAllActiveDocumentStandards(getEntityManager1());
        }

        return documentStandards;
    }

    public void doDocumentStandardSearch() {

        if (getIsActiveDocumentStandardsOnly()) {
            documentStandards = DocumentStandard.findActiveDocumentStandardsByAnyPartOfNameOrNumber(getEntityManager1(), standardSearchText);
        } else {
            documentStandards = DocumentStandard.findDocumentStandardsByAnyPartOfNameOrNumber(getEntityManager1(), standardSearchText);
        }

    }

    public void doComplaintSearch() {

        complaints = Complaint.findComplaintsByDateSearchField(
                getEntityManager1(),
                getUser(),
                "dateReceived",
                "General",
                complaintSearchText,
                null, null);

    }

    public void onDocumentStandardCellEdit(CellEditEvent event) {
        BusinessEntityUtils.saveBusinessEntityInTransaction(getEntityManager1(),
                getDocumentStandards().get(event.getRowIndex()));
    }

    public void onMarketProductCellEdit(CellEditEvent event) {
        BusinessEntityUtils.saveBusinessEntityInTransaction(getEntityManager1(),
                getMarketProducts().get(event.getRowIndex()));
    }

    public int getNumDocumentStandards() {
        return getDocumentStandards().size();
    }

    public int getNumMarketProducts() {
        if (marketProducts != null) {
            return getMarketProducts().size();
        } else {
            return 0;
        }
    }

    public void editCurrentDocumentStandard() {
        openDocumentStandardDialog();
    }

    public void editCurrentMarketProduct() {
        openMarketProductDialog();
    }

    public void editCurrentProductInspection() {
        openProductInspectionDialog();

        setEdit(true);
    }

    public void editCurrentComplaintProductInspection() {
        openComplaintProductInspectionDialog();

        setEdit(true);
    }

    public Boolean getIsNewDocumentStandard() {
        return getCurrentDocumentStandard().getId() == null;
    }

    public void okDocumentStandard() {

        try {

            // Update tracking
            if (getIsNewDocumentStandard()) {
                getCurrentDocumentStandard().setDateEntered(new Date());
                getCurrentDocumentStandard().setDateEdited(new Date());

                if (getUser() != null) {
                    //getCurrentDocumentStandard().setEnteredBy(getUser().getEmployee());
                    getCurrentDocumentStandard().setEditedBy(getUser().getEmployee());
                }
            }

            // Do save
            if (getCurrentDocumentStandard().getIsDirty()) {
                getCurrentDocumentStandard().setDateEdited(new Date());
                if (getUser() != null) {
                    getCurrentDocumentStandard().setEditedBy(getUser().getEmployee());
                }
                getCurrentDocumentStandard().save(getEntityManager1());
                getCurrentDocumentStandard().setIsDirty(false);
            }

            PrimeFaces.current().dialog().closeDynamic(null);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void okMarketProduct() {

        try {

            // Do save
            if (getCurrentMarketProduct().getIsDirty()) {

                getCurrentMarketProduct().save(getEntityManager1());
                getCurrentMarketProduct().setIsDirty(false);
            }

            PrimeFaces.current().dialog().closeDynamic(null);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void cancelDocumentStandardEdit() {
        getCurrentDocumentStandard().setIsDirty(false);

        PrimeFaces.current().dialog().closeDynamic(null);
    }

    public void cancelMarketProductEdit() {
        getCurrentMarketProduct().setIsDirty(false);

        PrimeFaces.current().dialog().closeDynamic(null);
    }

    public void updateDocumentStandard() {
        getCurrentDocumentStandard().setIsDirty(true);
    }

    public void updateMarketProduct() {
        getCurrentMarketProduct().setIsDirty(true);
    }

    public void updateDocumentStandardName(AjaxBehaviorEvent event) {
        getCurrentDocumentStandard().setName(getCurrentDocumentStandard().getName().trim());

        updateDocumentStandard();
    }

    public String getComplaintSearchText() {
        return complaintSearchText;
    }

    public void setComplaintSearchText(String complaintSearchText) {
        this.complaintSearchText = complaintSearchText;
    }

    public void createNewFactoryInspection() {
        currentFactoryInspection = new FactoryInspection();
        currentFactoryInspection.setInspectionDate(new Date());

        editFactoryInspection();
    }

    public FactoryInspection getCurrentFactoryInspection() {
        if (currentFactoryInspection == null) {
            return new FactoryInspection();
        }
        return currentFactoryInspection;
    }

    public void setCurrentFactoryInspection(FactoryInspection currentFactoryInspection) {
        this.currentFactoryInspection = currentFactoryInspection;
    }

    public void doFactoryInspectionSearch() {

        factoryInspections = FactoryInspection.findFactoryInspectionsByDateSearchField(
                getEntityManager1(),
                null,
                "General",
                factoryInspectionSearchText,
                null, null);
    }
}
