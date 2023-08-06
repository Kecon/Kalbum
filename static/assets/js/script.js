var playPromise;
var editing = false;

function loadAlbums() {
    fetch("albums/").then(function(response) {
        return response.json();
    }).then(function(data) {
        if (data != null) {
            var html = '<ul>';
            for (var i = 0; i < data.length; i++) {
                const albumData = data[i];
                html += '<li><a href="javascript:void(0);" data-albumId="' + albumData.id + '" class="album">' + albumData.name + '</a></li>';
            }
            html += '</ul>';
            document.getElementById("albums").innerHTML = html;

            const albums = document.getElementsByClassName("album");

            for (let i = 0; i < albums.length; i++) {
                albums[i].addEventListener("click", function() {
                    document.getElementById("selectAlbum").close();
                    showAlbum(this.getAttribute("data-albumId"));
                });
            }
        }
    })
    .catch((error) => {
        console.error("Error loading albums:", error);
    });
}

function showAlbum(albumId) {
    document.getElementById("albumId").value = albumId;
    reloadContents();
}

function createAlbum() {
    const albumName = document.getElementById("albumName").value;

    fetch("albums/", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
            name: albumName
        }),
    }).then(function(response) {
        return response.json();
    }).then(function(data) {
        if (data != null) {
            console.log("Album created successfully!", data);
            document.getElementById("albumName").value = "";
            loadAlbums();
        }
    }).catch((error) => {
        console.error("Error creating album: ", error);
    });
}

function reloadContents() {
    const albumId = document.getElementById("albumId").value;
    editing = false;

    if(albumId == null || albumId == "") {
        document.getElementById("main").innerHTML = "";
        return;
    }

    fetch("albums/" + albumId + "/contents/").then(function(response) {
        return response.json();
    }).then(function(data) {
        if (data != null) {
            var html = '<div class="items">';
            for (var i = 0; i < data.length; i++) {
                const contentData = data[i];
                const altFull = contentData.alt == null ? "" : "<h2>" + contentData.alt + "</h2>";
                const altText = contentData.text == null ? "" : "<p>" + contentData.text + "</p>";
                const alt = contentData.alt == null ? "" : contentData.alt;
                const url = 'albums/' + albumId + '/contents/' + contentData.src;

                if (contentData.contentType.startsWith("image")) {
                    html += '<article class="item">' + altFull + altText + '<img src="albums/' + albumId + '/contents/thumbnails/' + contentData.src + '" alt="' + alt + '" data-image="' + url + '" data-width="' + contentData.width + '" data-height="' + contentData.height + '" class="image" id="item' + i + '" data-text="' + contentData.text + '"></a></article>';
                } else if (contentData.contentType.startsWith("video")) {
                    html += '<article class="item">' + altFull + altText + '<img src="albums/' + albumId + '/contents/thumbnails/' + contentData.src + '" alt="' + alt + '" data-video="' + url + '" data-width="' + contentData.width + '" data-height="' + contentData.height + '" class="video" id="item' + i + '" data-text="' + contentData.text + '"></a></article>';
                }
            }
            html += '</div>';
            document.getElementById("main").innerHTML = html;

            const images = document.getElementsByClassName("image");

            for (let i = 0; i < images.length; i++) {
                images[i].addEventListener("click", function() {
                    showImage(this);
                });
            }

            const videos = document.getElementsByClassName("video");

            for (let i = 0; i < videos.length; i++) {
                videos[i].addEventListener("click", function() {
                    showVideo(this);
                });
            }
        }
    })
    .catch((error) => {
        console.error("Error loading contents for albumId " + albumId, error);
    });
}

function uploadFile() {
    const albumId = document.getElementById("albumId").value;
    const file = document.getElementById("fileInput").files[0]; // Get the selected file from the input field.

    if (!file) {
        alert('Please select a file.');
        return;
    }

    var formData = new FormData();
    formData.append("file", file);

    fetch("albums/" + albumId + "/contents/", {
        method: "POST",
        body: formData,
    }).then(function(response) {
        if (response != null) {
            console.log("File uploaded successfully!", response);
            document.getElementById("fileInput").value = "";
            reloadContents();
        }
    }).catch((error) => {
        console.error("Error uploading file: ", error);
    });
}

function previousImage() {
    closeViewImage();

    const prevItem = document.getElementById("image").getAttribute("data-prev");
    if (prevItem != "") {
        showItem(document.getElementById("item" + prevItem));
    }
}

function nextImage() {
    closeViewImage();

    const nextItem = document.getElementById("image").getAttribute("data-next");
    if (nextItem != "") {
        showItem(document.getElementById("item" + nextItem));
    }
}

function editImage() {

    if(document.getElementById("editImageProperties").classList.contains("hide")) {
        document.getElementById("editImageProperties").classList.remove("hide");
        const albumId = document.getElementById("albumId").value;
        const alt = document.getElementById("image").getAttribute("data-alt");
        const text = document.getElementById("image").getAttribute("data-text");

        document.getElementById("imageAlt").value = alt;
        document.getElementById("imageText").value = text;
        editing = true;
    } else {
        document.getElementById("editImageProperties").classList.add("hide");
        editing = false;
    }
}

function saveImage() {
    const albumId = document.getElementById("albumId").value;
    const src = document.getElementById("image").src;
    const alt = document.getElementById("imageAlt").value;
    const text = document.getElementById("imageText").value;

    fetch(src, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
            alt: alt,
            text: text,
        }),
    }).then(function(response) {
        closeViewImage();
        reloadContents();
        return response.json();
    }).then(function(data) {
    });
}

function deleteImage() {
    fetch(document.getElementById("image").src, {
        method: "DELETE",
    }).then(function(response) {
        closeViewImage();
        reloadContents();
        return response.json();
    }).then(function(data) {
    });
}

function cancelEditImage() {
    document.getElementById("editImageProperties").classList.add("hide");
    editing = false;
}

function downloadImage() {
    const url = document.getElementById("image").src;
    const filename = url.substring(url.lastIndexOf('/')+1);

    fetch(url).then(function(response) {
        return response.blob();
    }).then(function(data) {
        const a = document.createElement("a");
        document.body.appendChild(a);
        a.classList.add("hide");
        const blob = new Blob([data], {type: "octet/stream"}),
        url = window.URL.createObjectURL(blob);
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    })
    .catch((error) => {
        console.error("Error downloading image: ", error);
    });
}

function previousVideo() {
    const prevItem = document.getElementById("video").getAttribute("data-prev");
    closeViewVideo();

    if (prevItem != "") {
        setTimeout(function() {
            showItem(document.getElementById("item" + prevItem));
        }, 100);
    }
}

function nextVideo() {
    const nextItem = document.getElementById("video").getAttribute("data-next");
    closeViewVideo();

    if (nextItem != "") {
        setTimeout(function() {
            showItem(document.getElementById("item" + nextItem));
        }, 100);
    }
}

function editVideo() {

        if(document.getElementById("editVideoProperties").classList.contains("hide")) {
            document.getElementById("editVideoProperties").classList.remove("hide");
            const albumId = document.getElementById("albumId").value;
            const src = document.getElementById("videoSource").src;
            const alt = document.getElementById("video").getAttribute("data-alt");
            const text = document.getElementById("video").getAttribute("data-text");

            document.getElementById("videoAlt").value = alt;
            document.getElementById("videoText").value = text;
            editing = true;
        } else {
            document.getElementById("editVideoProperties").classList.add("hide");
            editing = false;
        }
}

function saveVideo() {
    const albumId = document.getElementById("albumId").value;
    const src = document.getElementById("videoSource").src;
    const alt = document.getElementById("videoAlt").value;
    const text = document.getElementById("videoText").value;

    fetch(src, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
            alt: alt,
            text: text,
        }),
    }).then(function(response) {
        closeViewVideo();
        reloadContents();
        return response.json();
    }).then(function(data) {
    });
}

function cancelEditVideo() {
    document.getElementById("editVideoProperties").classList.add("hide");
    editing = false;
}

function downloadVideo() {
    const url = document.getElementById("videoSource").src;
    const filename = url.substring(url.lastIndexOf('/')+1);

    fetch(url).then(function(response) {
        return response.blob();
    }).then(function(data) {
        const a = document.createElement("a");
        document.body.appendChild(a);
        a.classList.add("hide");
        const blob = new Blob([data], {type: "octet/stream"}),
        url = window.URL.createObjectURL(blob);
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    }).catch((error) => {
        console.error("Error downloading video: ", error);
    });
}

function deleteVideo() {
    fetch(document.getElementById("videoSource").src, {
        method: "DELETE",
    }).then(function(response) {
        closeViewVideo();
        reloadContents();
        return response.json();
    }).then(function(data) {
    });
}

function fadeIn(element) {
    element.classList.add("fadeIn");
}

function fadeInEvent(event) {
    event.target.classList.add("fadeIn");
}

function ready(fn) {
  if (document.readyState !== 'loading') {
    fn();
    return;
  }
  document.addEventListener('DOMContentLoaded', fn);
}

ready(function () {

    document.getElementById("createAlbumButton").addEventListener("click", createAlbum);

    document.getElementById("fileInput").addEventListener("change", uploadFile);

    document.getElementById("viewImage").getElementsByClassName("close")[0].addEventListener("click", closeViewImage);
    document.getElementById("viewImage").getElementsByClassName("next")[0].addEventListener("click", nextImage);
    document.getElementById("viewImage").getElementsByClassName("previous")[0].addEventListener("click", previousImage);
    document.getElementById("viewImage").getElementsByClassName("download")[0].addEventListener("click", downloadImage);
    document.getElementById("viewImage").getElementsByClassName("edit")[0].addEventListener("click", editImage);
    document.getElementById("viewImage").getElementsByClassName("delete")[0].addEventListener("click", deleteImage);

    document.getElementById("viewImage").addEventListener("keydown", function(event) {
        if (event.key === "ArrowLeft" && !editing) {
            previousImage();
            event.preventDefault();
        } else if(event.key === "ArrowRight" && !editing) {
            nextImage();
            event.preventDefault();
        } else if(event.key === "Escape") {
            closeViewImage();
            event.preventDefault();
        } else if(event.key === "ArrowDown" && !editing) {
            downloadImage();
            event.preventDefault();
        } else if(event.key === "ArrowUp" && !editing) {
            editImage();
            event.preventDefault();
        } else if(event.key === "Delete" && !editing) {
            deleteImage();
            event.preventDefault();
        }
    });

    document.getElementById("video").addEventListener("loaded", (event) => fadeIn(document.getElementById("viewVideo")));
    document.getElementById("image").addEventListener("loaded", (event) => fadeIn(document.getElementById("viewImage")));

    document.getElementById("viewVideo").getElementsByClassName("close")[0].addEventListener("click", closeViewVideo);
    document.getElementById("viewVideo").getElementsByClassName("next")[0].addEventListener("click", nextVideo);
    document.getElementById("viewVideo").getElementsByClassName("previous")[0].addEventListener("click", previousVideo);
    document.getElementById("viewVideo").getElementsByClassName("download")[0].addEventListener("click", downloadVideo);
    document.getElementById("viewVideo").getElementsByClassName("edit")[0].addEventListener("click", editVideo);
    document.getElementById("viewVideo").getElementsByClassName("delete")[0].addEventListener("click", deleteVideo);

    document.getElementById("viewVideo").addEventListener("close", (event) => {
        if (playPromise !== undefined) {
            playPromise.then(_ => {
              console.log("Pause video");
              video.pause();
            })
            .catch(error => {
                console.log(error);
            });
          }

        document.getElementById("videoSource").src = "";
    });

    document.getElementById("viewVideo").addEventListener("keydown", function(event) {
        if (event.key === "ArrowLeft" && !editing) {
            previousVideo();
            event.preventDefault();
        } else if(event.key === "ArrowRight" && !editing) {
            nextVideo();
            event.preventDefault();
        } else if(event.key === "Escape") {
            closeViewVideo();
            event.preventDefault();
        } else if(event.key === "ArrowDown" && !editing) {
            downloadVideo();
            event.preventDefault();
        } else if(event.key === "ArrowUp" && !editing) {
            editVideo();
            event.preventDefault();
        } else if(event.key === "Delete" && !editing) {
            deleteVideo();
            event.preventDefault();
        }
    });

    document.getElementById("selectAlbumButton").addEventListener("click", showSelectAlbumDialog);

    document.getElementById("selectAlbum").getElementsByClassName("close")[0].addEventListener("click", function() {
        document.getElementById("selectAlbum").close();
    });

    document.getElementById("saveImage").addEventListener("click", saveImage);
    document.getElementById("cancelImage").addEventListener("click", cancelEditImage);

    document.getElementById("saveVideo").addEventListener("click", saveVideo);
    document.getElementById("cancelVideo").addEventListener("click", cancelEditVideo);
});



function closeViewImage() {
    document.getElementById("viewImage").close();
    document.getElementById("image").src = "";
    document.getElementById("viewImage").classList.remove("fadeIn");
}

function closeViewVideo() {
    document.getElementById("viewVideo").close();
    document.getElementById("viewVideo").classList.remove("fadeIn");
}

function showItem(target) {
    if (target.classList.contains("image")) {
        showImage(target);
    } else if (target.classList.contains("video")) {
        showVideo(target);
    }

    target.scrollIntoView({ behavior: "smooth", block: "end", inline: "nearest" });
}

function showImage(target)
{
    document.getElementById("editImageProperties").classList.add("hide");

    document.getElementById("image").src = target.getAttribute("data-image");
    const currentItem = target.id.replace("item", "");
    const nextItem = parseInt(currentItem) + 1;
    const prevItem = parseInt(currentItem) - 1;

    const element = document.getElementById("image");

    element.setAttribute("data-current", currentItem);
    element.setAttribute("data-alt", target.alt);
    element.setAttribute("data-text", target.getAttribute("data-text"));

    if (document.getElementById("item" + nextItem) != null) {
        element.setAttribute("data-next", nextItem);
        document.getElementById("nextImage").classList.remove("hide");
    } else {
        document.getElementById("nextImage").classList.add("hide");
        element.setAttribute("data-next", "");
    }

    if (document.getElementById("item" + prevItem) != null) {
        element.setAttribute("data-prev", prevItem);
        document.getElementById("prevImage").classList.remove("hide");
    } else {
        document.getElementById("prevImage").classList.add("hide");
        element.setAttribute("data-prev", "");
    }

    const width = target.getAttribute("data-width");
    const height = target.getAttribute("data-height");
    const viewImage = document.getElementById("viewImage");
    const maxWidth = window.innerWidth * 0.95;
    const maxHeight = window.innerHeight * 0.95;
    const ratio = width / height;

    var newWidth;
    var newHeight;
    if (ratio > 1) {
        newWidth = width > maxWidth ? maxWidth : width;
        newHeight = newWidth / ratio;

        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = newHeight * ratio;
        }
    } else {
        newHeight = height > maxHeight ? maxHeight : height;
        newWidth = newHeight * ratio;

        if (newWidth > maxWidth ) {
            newWidth = maxWidth ;
            newHeight = newWidth / ratio;
        }
    }

    viewImage.style.width = newWidth + "px";
    viewImage.style.height = newHeight + "px";
    viewImage.style.maxWidth = newWidth + "px";
    viewImage.style.maxHeight = newHeight + "px";
    viewImage.style.overflow = 'hidden';

    viewImage.showModal();
    viewImage.classList.add("fadeIn");
    return false;
}

function showVideo(target)
{
    document.getElementById("editVideoProperties").classList.add("hide");

    document.getElementById("videoSource").src = target.getAttribute("data-video");
    document.getElementById("video").setAttribute("data-alt", target.alt);
    document.getElementById("video").setAttribute("data-text", target.getAttribute("data-text"));

    const currentItem = target.id.replace("item", "");
    const nextItem = parseInt(currentItem) + 1;
    const prevItem = parseInt(currentItem) - 1;

    const element = document.getElementById("video");

    element.setAttribute("data-current", currentItem);

    if (document.getElementById("item" + nextItem) != null) {
        element.setAttribute("data-next", nextItem);
        document.getElementById("nextVideo").classList.remove("hide");
    } else {
        element.setAttribute("data-next", "");
        document.getElementById("nextVideo").classList.add("hide");
    }

    if (document.getElementById("item" + prevItem) != null) {
        element.setAttribute("data-prev", prevItem);
        document.getElementById("prevVideo").classList.remove("hide");
    } else {
        element.setAttribute("data-prev", "");
        document.getElementById("prevVideo").classList.add("hide");
    }

    const width = target.getAttribute("data-width");
    const height = target.getAttribute("data-height");
    const viewVideo = document.getElementById("viewVideo");
    const maxWidth = window.innerWidth * 0.95;
    const maxHeight = window.innerHeight * 0.95;
    const ratio = width / height;

    var newWidth;
    var newHeight;
    if (ratio > 1) {
        newWidth = width > maxWidth ? maxWidth : width;
        newHeight = newWidth / ratio;

        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = newHeight * ratio;
        }
    } else {
        newHeight = height > maxHeight ? maxHeight : height;
        newWidth = newHeight * ratio;

        if (newWidth > maxWidth ) {
            newWidth = maxWidth ;
            newHeight = newWidth / ratio;
        }
    }

    viewVideo.style.width = newWidth + "px";
    viewVideo.style.height = newHeight + "px";
    viewVideo.style.maxWidth = newWidth + "px";
    viewVideo.style.maxHeight = newHeight + "px";
    viewVideo.style.overflow = 'hidden';
    viewVideo.showModal();
    document.getElementById("video").load();
    playPromise = document.getElementById("video").play();
    viewVideo.classList.add("fadeIn");
    return false;
}

function showSelectAlbumDialog() {
   loadAlbums();
   document.getElementById("selectAlbum").showModal();
   document.getElementById("selectAlbum").classList.add("fadeIn");
   return false;
}
