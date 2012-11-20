$(function() {
    $("input[id=location-sort]:radio").change(function() {

        $("input.location-input").show();
    }) 

    $("input[id!=location-sort]:radio").change(function() {

        $("input.location-input").val("");
        $("input.location-input").hide();
        
    })
});
