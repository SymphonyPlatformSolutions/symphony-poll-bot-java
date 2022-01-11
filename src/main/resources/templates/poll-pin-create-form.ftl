<form id="poll-create-form" multi-submit="reset">
    <ui-action action="open-dialog" target-id="poll-dialog">
        <button>Create Poll</button>
    </ui-action>
    <dialog id="poll-dialog" width="large">
        <title>
            <div style='display:flex'>
                <div style="padding-right:.5rem">
                    <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAACXBIWXMAAAsTAAALEwEAmpwYAAABt0lEQVQ4jY3UO2tVQRQF4O8GK8EHSFDDhYgoahXxgVEwoFViJYKg2Gh1GmsLCxshPyBiYadYWcUqik/Qxgc+CoWYIgpiUMgV0UYEHYuZmzs5j3AXDOecvWevWXtmzWmFEEDrmhwjOI5xbMfqFP+FD5jBbcxCKHqFrRLhFtzHNv3hJSZCodMNDGTJ83hfQ/YPi/iOUMrtx6M8kBPu0WsNpjGBzRjCJrRxAveyeUM54aqGVt6kwjIW0kLT+IThsuqBag342xDP8Ts9W/0QDuMw1q5AOIrLYjdLaGp5EE/S+2Ncx1N8zOb8wKVyYa7wHeZV2z2CGym3gMmkbk2dkrIPiW2exknsxoaGLoJ42lOhMLMSYRmjOIej2Kq674uhMNj9yPdwTPTaA9HEXTxLQ1J7BqdwMFO6hHy1s7iFDq42qO1gCodEH1bQZJuxhniOP+nZlw/baTRhRO/OL3NFTvgQX9P7enzGFazL5mzETbzNanNvVk55HHdqFF1Mi1yoye0LhVd1CuEuDmCuFJ+sIXuNnfTI6gjhBXaIv64vNflZ0Zt7xb/3MjQdSldtW7w1P/ENx7ALz5uK/gOYQmfdlkUDLAAAAABJRU5ErkJggg==" />
                </div>
                <b>Create New Poll</b>
            </div>
        </title>
        <body>
            <h5>Question</h5>
            <textarea name="question" placeholder="Enter your poll question.." required="true"></textarea>

            <h5>Answers</h5>
            <#list (1..data.count)?chunk(2) as row>
                <div style="display:flex">
                    <#list row as option>
                        <#if option < 3 >
                            <#assign required="true">
                        <#else>
                            <#assign required="false">
                        </#if>
                        <div style="width:16rem;padding-right:1rem">
                            <text-field name="option${option}" label="Option ${option}" required="${required}" />
                        </div>
                    </#list>
                </div>
            </#list>

            <#if data.showPersonSelector>
                <h6>Audience</h6>
                <person-selector name="audience" placeholder="Select audience.." required="true" />
            </#if>

            <#if data.hideStreamId>
                <div style="display:none">
            </#if>
                <h6>Room Stream ID</h6>
                <text-field name="targetStreamId" placeholder="Enter room stream id.."
                    required="true">${data.targetStreamId}</text-field>
            <#if data.hideStreamId>
                </div>
            </#if>

            <h6>Expiry (in minutes)</h6>
            <text-field name="timeLimit" pattern="\d+" pattern-error-message="Enter a number" title="0 denotes no limit\nMax 1440 for 24 hours">0</text-field>
        </body>
        <footer>
            <button name="createPoll" type="action">Create Poll</button>
        </footer>
    </dialog>
</form>
