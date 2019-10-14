<div class='entity' data-entity-id='poll'>
    <div style='display:flex;padding-top:8px'>
        <div><img src="https://symphony.com/wp-content/uploads/2019/08/favicon.png" style='height:20px' /></div>
        <div style='padding-top:1px;padding-left:5px;'>
            <b>Last 10 Polls</b> from: <mention uid="${entity["poll"].creatorId}" />
            <#if entity["poll"].room == true>
                in this room
            </#if>
        </div>
    </div>

    <div style='height:2px;background:#0098ff;margin-top:10px;margin-bottom:10px'> </div>

    <table>
        <tr>
            <th>Question</th>
            <th>Created/Ended (UTC)</th>
            <th>Answer</th>
            <th style="text-align:right">Votes</th>
            <th></th>
        </tr>
        <tr style="height: 1px"><td colspan="5" style="padding:0;background: #bbb"></td></tr>
        <#list entity["poll"].polls as poll>
            <tr>
                <td rowspan="${poll.results?size}">${poll.questionText}</td>
                <td rowspan="${poll.results?size}">${poll.created}<br/>${poll.ended}</td>
                <td>${poll.results[0].answer}</td>
                <td style="text-align:right">${poll.results[0].count}</td>
                <td><div style='background-color:#29b6f6;width:${poll.results[0].width}px'> </div></td>
            </tr>
            <#list poll.results as result>
                <#if result?index gt 0>
                    <tr>
                        <td>${result.answer}</td>
                        <td style="text-align:right">${result.count}</td>
                        <td><div style='background-color:#29b6f6;width:${result.width}px'> </div></td>
                    </tr>
                </#if>
            </#list>
            <tr style="height: 1px"><td colspan="5" style="padding:0;background: #bbb"></td></tr>
        </#list>
    </table>
</div>
