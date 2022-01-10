<div style='display:flex'>
    <div style="padding-right:.5rem">
        <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAACXBIWXMAAAsTAAALEwEAmpwYAAABt0lEQVQ4jY3UO2tVQRQF4O8GK8EHSFDDhYgoahXxgVEwoFViJYKg2Gh1GmsLCxshPyBiYadYWcUqik/Qxgc+CoWYIgpiUMgV0UYEHYuZmzs5j3AXDOecvWevWXtmzWmFEEDrmhwjOI5xbMfqFP+FD5jBbcxCKHqFrRLhFtzHNv3hJSZCodMNDGTJ83hfQ/YPi/iOUMrtx6M8kBPu0WsNpjGBzRjCJrRxAveyeUM54aqGVt6kwjIW0kLT+IThsuqBag342xDP8Ts9W/0QDuMw1q5AOIrLYjdLaGp5EE/S+2Ncx1N8zOb8wKVyYa7wHeZV2z2CGym3gMmkbk2dkrIPiW2exknsxoaGLoJ42lOhMLMSYRmjOIej2Kq674uhMNj9yPdwTPTaA9HEXTxLQ1J7BqdwMFO6hHy1s7iFDq42qO1gCodEH1bQZJuxhniOP+nZlw/baTRhRO/OL3NFTvgQX9P7enzGFazL5mzETbzNanNvVk55HHdqFF1Mi1yoye0LhVd1CuEuDmCuFJ+sIXuNnfTI6gjhBXaIv64vNflZ0Zt7xb/3MjQdSldtW7w1P/ENx7ALz5uK/gOYQmfdlkUDLAAAAABJRU5ErkJggg==" />
    </div>
    <h5>Poll Results: ${data.question} by ${data.creatorDisplayName}</h5>
</div>

<div style='height:.2rem;background:#0098ff;margin:.5rem 0'> </div>

<table>
    <tr>
        <th>Answer</th>
        <th style="text-align:right">Votes</th>
        <th></th>
    </tr>
    <#list data.results as result>
        <tr>
            <td>${result.answer}</td>
            <td style="text-align:right">${result.count}</td>
            <td><div style='background-color:#29b6f6;width:${result.width}px'> </div></td>
        </tr>
    </#list>
</table>
