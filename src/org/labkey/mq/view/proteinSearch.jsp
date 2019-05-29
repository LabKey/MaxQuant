<%@ page import="org.labkey.api.ms2.MS2Urls" %>
<%@ page import="org.labkey.api.protein.ProteinService" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ProteinService.ProteinSearchForm> me = (JspView<ProteinService.ProteinSearchForm>) HttpView.currentView();
    ProteinService.ProteinSearchForm bean = me.getModelBean();
%>

<labkey:form layout="horizontal" action="<%= h(urlProvider(MS2Urls.class).getProteinSearchUrl(getContainer())) %>">
    <labkey:input type="text"
                  id="identifierInput"
                  name="identifier"
                  value="<%=h(bean.getIdentifier())%>"
                  label="Protein label *"
                  forceSmallContext="true"
                  contextContent="Search within the following fields of the Protein Group table: Protein ID, Majority Protein ID, Protein Name, and Gene Name. You can enter a comma separated list for multiple value search."
    />
    <labkey:input type="checkbox"
                  id="includeSubfoldersInput"
                  name="includeSubfolders"
                  checked="<%=bean.isIncludeSubfolders()%>"
                  label="Search in subfolders"
                  forceSmallContext="true"
                  contextContent="If checked, the search will also look in all of this folder's children."
    />
    <labkey:input type="checkbox"
                  id="exactMatchInput"
                  name="exactMatch"
                  label="Exact matches only"
                  checked="<%=bean.isExactMatch()%>"
                  forceSmallContext="true"
                  contextContent="If checked, the search will only find proteins with an exact label match. If not checked, proteins that contain the label entered will also match, but the search may be significantly slower."
    />
    <input type="hidden" name="showMatchingProteins" value="false">
    <input type="hidden" name="showProteinGroups" value="false">
    <br/>
    <labkey:button text="Search" />
</labkey:form>


