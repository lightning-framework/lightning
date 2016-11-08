<#escape x as x?html>
<!DOCTYPE html>
<html>
    <head>
        <title>Lightning Route Overview</title>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="theme-color" content="#CC2222">
        <meta name="apple-mobile-web-app-status-bar-style" content="#CC2222">
        <script><#include "/public/js/lib/jquery-3.1.1.min.js"></script>
        <script type="text/javascript">
        $(document).ready(function() {
            $("#star-frame").attr("src", "https://ghbtns.com/github-btn.html?user=lightning-framework&repo=lightning&type=star&count=true").css("width", "95px");

            function handleResponse(data) {

                window.console.log(data);
                var form = $("form[name=route_lookup]");
                var reply = form.find(".reply");

                if (data['status'] == 'success') {
                    if (data['matches'].length > 0) {
                        reply.text(data['matches'].join("\n"));
                    } else {
                        reply.text('No matches (NotFoundException handler will be called).');
                    }
                } else {
                    reply.text('Error: ' + data['type'] + ': ' + data['message']);
                }
            }

            function disableForm() {
                $("form[name=route_lookup] input").attr("disabled", "disabled");
                $("form[name=route_lookup] select").attr("disabled", "disabled");
            }

            function enableForm() {
                $("form[name=route_lookup] input").removeAttr("disabled");
                $("form[name=route_lookup] select").removeAttr("disabled");
            }

            $("form[name=route_lookup]").on('submit', function(e) {
                e.preventDefault();
                $.ajax({
                    url: "",
                    type: "POST",
                    accepts: "application/json",
                    dataType: "json",
                    contentType: "application/x-www-form-urlencoded; charset=UTF-8",
                    data: $(this).serialize(),
                    async : true,
                    cache: false,
                    timeout: 3000,
                    success: function(data) {
                        handleResponse(data);
                        enableForm();
                    },
                    error: function() {
                        handleResponse({'status': 'error', 'type': 'IOError', 'message': 'Failed to connect to endpoint.'});
                        enableForm();
                    }
                });
                disableForm();
            });

            function updateHeight() {
                $(".container").css({"height": ($(window).height() - $("#spark-header").height()) + "px",
                                     "overflow": "auto"});
            }

            $(window).on('resize', function(event) {
                updateHeight();
            });

            updateHeight();
        });
        </script>
        <style type="text/css"><#include "/public/css/main.css"></style>
        <style type="text/css"><#include "/public/css/code.css"></style>
        <style type="text/css"><#include "/public/css/exception-buttons.css"></style>
    </head>
    <body>
        <div id="spark-header">
            <h1>Lightning Framework: <span style="font-style: normal;">Route Overview<span></h1>
            <ul>
                <li><a href="https://lightning-framework.github.io/#info" target="_blank">View Documentation</a></li>
                <li><a href="https://github.com/lightning-framework/examples" target="_blank">View Examples</a></li>
            </ul>
            <div id="some-buttons">
                <div id="github-star">
                    <img id="star-bg" src="data:image/png;base64,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">
                    <iframe id="star-frame" src="" frameborder="0" scrolling="0" width="95px" height="20px"></iframe>
                </div>
            </div>
        </div>
        <div class="container">
            <div class="route-map-container">

                <form method="POST" name="route_lookup">
                    <p>Enter a path to see what routes and filters it will match.</p>
                    <select name="method">
                        <#list http_methods as method>
                            <option>${method}</option>
                        </#list>
                    </select>
                    <input type="text" name="path" value="/" />
                    <input type="submit" value="Go" />
                    <div class="reply"></div>
                </form>
                <hr />

                <div class="route-map-table">
                    <div class="label">Routes</div>
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 35%">Path</th>
                                <th style="width: 15%">Methods</th>
                                <th style="width: 50%">Handler</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list routes as handler>
                                <tr>
                                    <td><code>${handler.path}</code></td>
                                    <td>${handler.methods}</td>
                                    <td>
                                        <#if handler.filters?has_content>
                                            <#list handler.filters as filter>
                                                <code>${filter}</code>
                                            </#list>
                                        </#if>
                                        <code>${handler.target}</code>
                                    </td>
                                </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>

                <div class="route-map-table">
                    <div class="label">Path-Based Filters</div>
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 35%;">Path</th>
                                <th style="width: 15%;">Methods</th>
                                <th style="width: 50%;">Handler</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list before_filters as handler>
                                <tr>
                                    <td><code>${handler.path}</code></td>
                                    <td>${handler.methods}</td>
                                    <td><code>${handler.target}</code></td>
                                </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>

                <div class="route-map-table">
                    <div class="label">Web Sockets</div>
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 50%;">Path</th>
                                <th style="width: 50%;">Handler</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list websockets as handler>
                                <tr>
                                    <td><code>${handler.path}</code></td>
                                    <td><code>${handler.target}</code></td>
                                </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>

                <div class="route-map-table">
                    <div class="label">Exception Handlers</div>
                    <table>
                        <thead>
                            <tr>
                                <th style="width: 50%;">Exception Type</th>
                                <th style="width: 50%;">Handler</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list exception_handlers as handler>
                                <tr>
                                    <td><code>${handler.exception}</code></td>
                                    <td><code>${handler.target}</code></td>
                                </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>

            </div>
        </div>
    </body>
</html>
</#escape>
