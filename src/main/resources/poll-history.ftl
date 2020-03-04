<card accent="tempo-bg-color--blue">
    <header>
        <div style='display:flex;padding-top:8px'>
            <div><img src="https://symphony.com/wp-content/uploads/2019/08/favicon.png" style='height:20px' /></div>
            <div style='padding-top:1px;padding-left:5px;'>
                <b>Last 10 Polls</b> from: <b>${data.creatorDisplayName}</b>
                <#if data.room == true>
                    in this room
                </#if>
            </div>
        </div>
    </header>
    <body>
        <table>
            <tr>
                <th>Created/Ended (UTC)</th>
                <th>Question</th>
                <th>Answer</th>
                <th style="text-align:right">Votes</th>
                <th></th>
            </tr>
            <tr style="height: 1px"><td colspan="5" style="padding:0;background: #bbb"></td></tr>
            <#list data.polls as poll>
                <tr>
                    <td rowspan="${poll.results?size}">${poll.created}<br/>${poll.ended}</td>
                    <td rowspan="${poll.results?size}">${poll.questionText}</td>
                    <td>${poll.results[0].answer}</td>
                    <td style="text-align:right">${poll.results[0].count}</td>
                    <td><div style='background-color:#29b6f6;height:20px;width:${poll.results[0].width}px'> </div></td>
                </tr>
                <#list poll.results as result>
                    <#if result?index gt 0>
                        <tr>
                            <td>${result.answer}</td>
                            <td style="text-align:right">${result.count}</td>
                            <td><div style='background-color:#29b6f6;height:20px;width:${result.width}px'> </div></td>
                        </tr>
                    </#if>
                </#list>
                <tr style="height: 1px"><td colspan="5" style="padding:0;background: #bbb"></td></tr>
            </#list>
        </table>
    </body>
</card>
