
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.ms2.MS2Urls" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<labkey:form layout="horizontal" action="<%= h(PageFlowUtil.urlProvider(MS2Urls.class).getProteinSearchUrl(getContainer())) %>">
    <labkey:input type="text"
                  id="identifierInput"
                  name="identifier"
                  label="Protein label *"
                  forceSmallContext="true"
                  contextContent="Search within the following fields of the Protein Group table: Protein ID, Majority Protein ID, Protein Name, and Gene Name. You can enter a comma separated list for multiple value search."
    />
    <labkey:input type="checkbox"
                  id="includeSubfoldersInput"
                  name="includeSubfolders"
                  label="Search in subfolders"
                  forceSmallContext="true"
                  contextContent="If checked, the search will also look in all of this folder's children."
    />
    <labkey:input type="checkbox"
                  id="exactMatchInput"
                  name="exactMatch"
                  label="Exact matches only"
                  checked="true"
                  forceSmallContext="true"
                  contextContent="If checked, the search will only find proteins with an exact label match. If not checked, proteins that contain the label entered will also match, but the search may be significantly slower."
    />
    <br/>
    <labkey:button text="Search" />
</labkey:form>


