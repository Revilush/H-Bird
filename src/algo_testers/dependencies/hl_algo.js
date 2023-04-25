function hl_main(sent1,sent2,client_ht2w,var_ht2w){

var lcsdif = new lcsDiff('');

var diffjson = lcsdif.getDiffJson(sent2, sent1 );

var client_ht2w_sub = client_ht2w.slice(0)
var clause_ht2w_sub = var_ht2w.slice(0)


var clause_ht2w_main = []
for (var m=0;m<clause_ht2w_sub.length;m++){
  if(client_ht2w_sub.includes(clause_ht2w_sub[m]) == true){
    var index = client_ht2w_sub.indexOf(clause_ht2w_sub[m]);
    client_ht2w_sub.splice(index,1)
  }
  else{
    clause_ht2w_main.push(clause_ht2w_sub[m])
  }
}
var client_ht2w_main = client_ht2w_sub

var teststr = ""

$.each(diffjson, function (idx,dif) {

  if (dif[0] == "+") {

    var word_array = (dif[2].toString()).split(/(\s+)/);

    for (var i = 0; i<word_array.length; i++) {
      var sub_text = word_array[i];

      for(var k =0;k<client_ht2w_main.length;k++){
        var regty = new RegExp("^" + client_ht2w_main[k]);
        if (regty.test(sub_text) == true){
          client_ht2w_main.splice(k,1)
          teststr += "<span style='color:#FF0000;'><u>"+dif[2] +"</u></span>"
          var col_text = true
          break;
        }
        else{
          var col_text = false
        }
      }
      if(col_text){
        break;
      }
    }
    if(!col_text){
      teststr += "<span>"+dif[2] +"</span>"
    }

  } else if (dif[0] == "=") {

    var word_array = (dif[2].toString()).split(/(\s+)/);
    teststr += "<span>"+dif[2] +"</span>"
  }
});

return teststr;

}

