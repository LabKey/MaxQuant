
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.mq.model.ExperimentGroup" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ExperimentGroup> me = (JspView<ExperimentGroup>) HttpView.currentView();
    ExperimentGroup bean = me.getModelBean();
%>
<form class="form-horizontal form-mode-details">
    <table>
        <tr>
            <td class="lk-form-label">Created:</td>
            <td><%=formatDateTime(bean.getCreated())%></td>
        </tr>
        <tr>
            <td class="lk-form-label">Created By:</td>
            <td><%=h(bean.getCreatedBy())%></td>
        </tr>
        <tr>
            <td class="lk-form-label">Location On File System:</td>
            <td><%=h(bean.getLocationOnFileSystem())%></td>
        </tr>
        <tr>
            <td class="lk-form-label">Protein Groups:</td>
            <td><div id="lk-mq-proteingroups"><i class="fa fa-spinner fa-pulse"></i></div></td>
        </tr>
        <tr>
            <td class="lk-form-label">Peptides:</td>
            <td><div id="lk-mq-peptides"><i class="fa fa-spinner fa-pulse"></i></div></td>
        </tr>
    </table>
</form>

<script type="text/javascript">
    +function ($) {
        LABKEY.Query.selectRows({
            schemaName: 'mq',
            queryName: 'ExperimentGroupDetails',
            filterArray: [LABKEY.Filter.create('Id', <%=bean.getId()%>)],
            requiredVersion: '17.1',
            columns: 'Id, ProteinGroups, Peptides',
            success: function(data) {
                if (data.rows.length === 1) {
                    var row = data.rows[0];
                    showValue('lk-mq-proteingroups', row.data['ProteinGroups']);
                    showValue('lk-mq-peptides', row.data['Peptides']);
                }
                else {
                    showValue('lk-mq-proteingroups', null);
                    showValue('lk-mq-peptides', null);
                }
            },
            failure: function(response) {
                alert('Error: ' + response.exception);
                showValue('lk-mq-proteingroups', null);
                showValue('lk-mq-peptides', null);
            }
        });

        function showValue(id, rowValue) {
            if (rowValue) {
                $('#' + id).html('<a href="' + rowValue.url + '">' + LABKEY.Utils.encodeHtml(rowValue.formattedValue) + '</a>');
            }
            else {
                $('#' + id).html('<span class="labkey-error">Error retrieving value</span>');
            }
        }
    }(jQuery);
</script>