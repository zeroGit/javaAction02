<head>
    <jsp:directive.include file="/WEB-INF/jsp/prelude/include-head-meta.jspf" />
    <jsp:directive.include file="/WEB-INF/jsp/prelude/include-css.jspf" />
    <jsp:directive.include file="/WEB-INF/jsp/prelude/include-js.jspf" />
</head>
<body style="height:100%">

<div class="container h-100">
  <div class="row align-items-center h-100">
  <div class="col-lg-4 col-md-4 mb-4 mx-auto">
    <img src="/static/img/h.png" />
    <p>—— 论成败，人生豪迈，大不了，重头再来。</p>
    <p>—— ${requestScope.errStr}</p>
  </div>
  </div>
</div>

</body>