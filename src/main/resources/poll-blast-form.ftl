<form id="poll-blast-form-${data.id}">
    <div style='display:flex;padding-top:8px'>
        <div><img src="https://symphony.com/wp-content/uploads/2019/08/favicon.png" style='height:20px' /></div>
        <div style='padding-top:1px;padding-left:5px;'>
            <b>Poll: ${data.question}</b> by <mention uid="${data.creatorId}" />
        </div>
    </div>

    <div style='height:2px;background:#0098ff;margin-top:10px;margin-bottom:10px'> </div>

    <#list data.answers as answer>
        <button name="option-${answer?index}" type="action">${answer}</button>
    </#list>

    <div style='height:1px;background:#0098ff;margin-top:10px;margin-bottom:10px'> </div>

    <i>This poll
        <#if data.timeLimit == 0>
            does not have a time limit
        <#else>
            will end in ${data.timeLimit} minute<#if data.timeLimit gt 1>s</#if>
        </#if>
    </i>
</form>
