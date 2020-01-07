var versions = {
    'current' : '/current/userguide/html_single/Hibernate_User_Guide.html',
    '5.4' : '/5.4/userguide/html_single/Hibernate_User_Guide.html',
    '5.3' : '/5.3/userguide/html_single/Hibernate_User_Guide.html',
    '5.2' : '/5.2/userguide/html_single/Hibernate_User_Guide.html',
    '5.1' : '/5.1/userguide/html_single/Hibernate_User_Guide.html',
    '5.0' : '/5.0/userguide/html_single/Hibernate_User_Guide.html',
    '4.3' : '/4.3/manual/en-US/html_single/',
    '4.2' : '/4.2/manual/en-US/html_single/',
    '4.1' : '/4.1/manual/en-US/html_single/',
    '4.0' : '/4.0/manual/en-US/html_single/',
    '3.6' : '/3.6/reference/en-US/html_single/',
    '3.5' : '/3.5/reference/en-US/html_single/',
    '3.3' : '/3.3/reference/en-US/html_single/',
    '3.2' : '/3.2/reference/en/html_single/'
};

$(document).ready(function() {
    $('#toctitle').before('<select id="vchooser"></select>');
    $('#vchooser').append('<option>Choose version</option>');

    for(var version in versions) {
        var path = 'http://docs.jboss.org/hibernate/orm' + versions[version];
        $('#vchooser').append('<option value="' + path + '" ' + '>' + version + '</option>');
    };

    $('#vchooser').change(function(e) {
        if (this.value !== '')
            window.location.href = this.value;
    });

    $('ul.sectlevel1').wrap('<div id="toctree"></div>');

    $('#toctree').jstree({
         "core" : {
             "themes" : {"variant" : "small", "icons" : false}
         },
         "plugins" : [ "search", "state", "wholerow" ]
    })
    .on("activate_node.jstree", function (e, data) { location.href = data.node.a_attr.href; });

    $('#toctree').before('<input placeholder="&#xf002; Search" id="tocsearch" type="text">');
    var searchTimeout = false;
    $('#tocsearch').keyup(function () {
        if(searchTimeout) { clearTimeout(searchTimeout); }
        searchTimeout = setTimeout(function () {
            var v = $('#tocsearch').val();
            $('#toctree').jstree(true).search(v);
        }, 250);
    });
    $('#tocsearch').after('<a href="#" id="toctreeexpand" title="Expand"><i class="fa fa-plus-square" aria-hidden="true"></i></a><a href="#" id="toctreecollapse" title="Collapse"><i class="fa fa-minus-square" aria-hidden="true"></i></a>');
    $('#toctreeexpand').click(function() { $('#toctree').jstree('open_all'); });
    $('#toctreecollapse').click(function() { $('#toctree').jstree('close_all'); });
});
