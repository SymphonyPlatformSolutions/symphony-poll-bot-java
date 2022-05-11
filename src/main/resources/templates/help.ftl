<table>
    <tr>
        <td><mention uid="${userId}" /><b>/poll</b></td>
        <td>Get a standard create poll form</td>
    </tr>
    <tr>
        <td><mention uid="${userId}" /> <b>/pin</b></td>
        <td>Creates a button that launches a create poll form and pins it to the chat</td>
    </tr>
    <tr>
        <td><mention uid="${userId}" /> <b>/poll room_stream_id</b></td>
        <td>Get a create poll form that targets a room</td>
    </tr>
    <tr>
        <td><mention uid="${userId}" /> <b>/poll 8</b></td>
        <td>Get a create poll form with custom number of options (between 2 and 10)</td>
    </tr>
    <tr>
        <td><mention uid="${userId}" /> <b>/endpoll</b></td>
        <td>End your active poll</td>
    </tr>
    <tr>
        <td><mention uid="${userId}" /> <b>/history</b></td>
        <td>View your personal poll history<#if !isIM> for this room</#if></td>
    </tr>
    <tr>
        <td><mention uid="${userId}" /> <b>/active</b></td>
        <td>Preview the results of your active poll<#if !isIM> for this room</#if></td>
    </tr>
</table>
