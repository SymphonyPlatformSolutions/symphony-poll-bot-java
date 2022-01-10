<#if data.polls?size gt 1>
<card accent="tempo-bg-color--blue">
    <header>
        <div style='display:flex;padding-top:8px'>
            <div><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAACXBIWXMAAAsTAAALEwEAmpwYAAABt0lEQVQ4jY3UO2tVQRQF4O8GK8EHSFDDhYgoahXxgVEwoFViJYKg2Gh1GmsLCxshPyBiYadYWcUqik/Qxgc+CoWYIgpiUMgV0UYEHYuZmzs5j3AXDOecvWevWXtmzWmFEEDrmhwjOI5xbMfqFP+FD5jBbcxCKHqFrRLhFtzHNv3hJSZCodMNDGTJ83hfQ/YPi/iOUMrtx6M8kBPu0WsNpjGBzRjCJrRxAveyeUM54aqGVt6kwjIW0kLT+IThsuqBag342xDP8Ts9W/0QDuMw1q5AOIrLYjdLaGp5EE/S+2Ncx1N8zOb8wKVyYa7wHeZV2z2CGym3gMmkbk2dkrIPiW2exknsxoaGLoJ42lOhMLMSYRmjOIej2Kq674uhMNj9yPdwTPTaA9HEXTxLQ1J7BqdwMFO6hHy1s7iFDq42qO1gCodEH1bQZJuxhniOP+nZlw/baTRhRO/OL3NFTvgQX9P7enzGFazL5mzETbzNanNvVk55HHdqFF1Mi1yoye0LhVd1CuEuDmCuFJ+sIXuNnfTI6gjhBXaIv64vNflZ0Zt7xb/3MjQdSldtW7w1P/ENx7ALz5uK/gOYQmfdlkUDLAAAAABJRU5ErkJggg==" /></div>
            <div style='padding-top:1px;padding-left:5px;'>
                <b>Last ${data.polls?size} Polls</b> from: <b>${data.creatorDisplayName}</b>
                <#if data.room == true>
                    in this room
                </#if>
            </div>
        </div>
    </header>
    <body>
</#if>
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
<#if data.polls?size gt 1>
    </body>
</card>
</#if>
