<#escape x as x?html>
	<h2>Directory Listing</h2>
	<ul>
		<#list files as file>
			<li><a href="${file.link}">${file.name}</a></li>
		</#list>
	</ul>
</#escape>
