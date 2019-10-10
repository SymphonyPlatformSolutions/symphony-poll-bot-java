<div class='entity' data-entity-id='poll'>
    <div style='display:flex;padding-top:8px'>
        <div><img src="https://symphony.com/wp-content/uploads/2019/08/favicon.png" style='height:20px' /></div>
        <div style='padding-top:1px;padding-left:5px;'>
            <b>Poll History</b> for: <mention uid="${entity["poll"].creatorId}" />
            <#if entity["poll"].room == true>
                for this room
            </#if>
        </div>
    </div>

    <div style='height:2px;background:#0098ff;margin-top:10px;margin-bottom:10px'> </div>

    <table>
        <tr>
            <th>Question</th>
            <th>Created</th>
            <th>Ended</th>
        </tr>
        <#list entity["poll"].polls as poll>
            <tr>
                <td>${poll.questionText}</td>
                <td>${poll.created}</td>
                <td>${poll.ended}</td>
            </tr>
        </#list>
    </table>
</div>
